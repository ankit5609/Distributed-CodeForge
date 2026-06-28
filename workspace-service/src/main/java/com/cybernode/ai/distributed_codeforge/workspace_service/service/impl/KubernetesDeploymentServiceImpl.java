package com.cybernode.ai.distributed_codeforge.workspace_service.service.impl;

import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeployResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeploymentLogsResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.DeploymentService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesDeploymentServiceImpl implements DeploymentService {

    private final KubernetesClient client;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.preview.namespace}")
    private String namespace;

    @Value("${app.preview.domain}")
    private String baseDomain;

    @Value("${app.preview.proxy-port}")
    private String proxyPort;

    private static final String POOL_LABEL = "status";
    private static final String PROJECT_LABEL = "project-id";
    private static final String IDLE = "idle";
    private static final String BUSY = "busy";

    @Override
    public DeployResponse deploy(Long projectId, boolean force) {
        // Dynamically build the domain: project-123.app.domain.com
        String domain = "project-" + projectId + "." + baseDomain;

        // Use default port 80 format logic for clean URLs, or explicit ports for local testing
        String formattedUrl = proxyPort.equals("80")
                ? "http://" + domain
                : "http://" + domain + ":" + proxyPort;

        Pod existingPod = findClaimedPod(projectId);

        if (existingPod != null) {
            String podName = existingPod.getMetadata().getName();
            String phase = existingPod.getStatus().getPhase();
            if (!"Running".equals(phase)) {
                log.info("Found existing pod {} for project {} in phase {}. Waiting for boot...", podName, projectId, phase);
                return new DeployResponse(formattedUrl);
            }

            // Bypasses reinstall/restart if the preview server is already active and force is false (e.g. silent pre-warm load)
            if (!force) {
                boolean isViteRunning = isProcessRunning(podName, "vite");
                if (isViteRunning) {
                    log.info("Vite dev server is already running on pod {} for project {}. Bypassing dev server restart.", podName, projectId);
                    registerRoute(domain, existingPod);
                    return new DeployResponse(formattedUrl);
                }
            }

            log.info("Found existing pod {} for project {}. Resuming and updating server...", podName, projectId);
            registerRoute(domain, existingPod);
            
            try {
                // Step 1: npm install (blocking - wait for it to fully finish)
                execCommand(podName, "runner", "sh", "-c", "npm install --no-audit --no-fund --prefer-offline");
                // Step 2: Kill existing Vite by port (fuser kills the process holding 5173/tcp)
                execCommand(podName, "runner", "sh", "-c", "fuser -k 5173/tcp 2>/dev/null || pkill -9 -f vite 2>/dev/null || true; sleep 1");
                // Step 3: Start fresh Vite in background
                execCommand(podName, "runner", "sh", "-c", "nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &");
            } catch (Exception e) {
                log.warn("Failed to restart dev server on existing pod {}, attempting clean redeploy...", podName, e);
                client.pods().inNamespace(namespace).withName(podName).delete();
                return claimAndStartNewPod(projectId, domain, formattedUrl);
            }
            
            return new DeployResponse(formattedUrl);
        }

        return claimAndStartNewPod(projectId, domain, formattedUrl);
    }

    private Pod findClaimedPod(Long projectId) {
        return client.pods().inNamespace(namespace)
                .withLabel(PROJECT_LABEL, projectId.toString())
                .withLabel(POOL_LABEL, BUSY)
                .list().getItems().stream()
                .findFirst()
                .orElse(null);
    }



    private DeployResponse claimAndStartNewPod(Long projectId, String domain, String formattedUrl) {
        Pod pod = client.pods().inNamespace(namespace)
                .withLabel(POOL_LABEL, IDLE)
                .list().getItems().stream()
                .findFirst()
                .orElse(null);
        if (pod == null) {
            log.info("No idle runners. Recyling oldest busy runner...");
            // Find the oldest busy pod (sorted by creation/start time or age)
            Pod oldestBusyPod = client.pods().inNamespace(namespace)
                    .withLabel(POOL_LABEL, BUSY)
                    .list().getItems().stream()
                    .min((p1, p2) -> p1.getMetadata().getCreationTimestamp().compareTo(p2.getMetadata().getCreationTimestamp()))
                    .orElseThrow(() -> new RuntimeException("No runners available in the pool."));

            // Extract project ID from the label and release it
            String oldestProjectIdStr = oldestBusyPod.getMetadata().getLabels().get(PROJECT_LABEL);
            if (oldestProjectIdStr != null) {
                log.info("Evicting project {} to free up pod {}", oldestProjectIdStr, oldestBusyPod.getMetadata().getName());
                release(Long.parseLong(oldestProjectIdStr));
            }

            pod = oldestBusyPod; // Reuse this pod
        }
        String podName = pod.getMetadata().getName();
        log.info("Claiming pod {} for project {}", podName, projectId);

        client.pods().inNamespace(namespace).withName(podName).edit(p -> {
            p.getMetadata().getLabels().put(POOL_LABEL, BUSY);
            p.getMetadata().getLabels().put(PROJECT_LABEL, projectId.toString());
            return p;
        });

        try {
            killExistingWatchers(podName);
            String initialSyncCmd = String.format("for f in /app/* /app/.[!.]*; do [ -e \"$f\" ] && [ \"$f\" != \"/app/node_modules\" ] && rm -rf \"$f\"; done && mc mirror --overwrite myminio/projects/%d/ /app/", projectId);
            execCommand(podName, "syncer", "sh", "-c", initialSyncCmd);

            String watchCmd = String.format("nohup mc mirror --overwrite --watch myminio/projects/%d/ /app/ > /app/sync.log 2>&1 &", projectId);
            execCommand(podName, "syncer", "sh", "-c", watchCmd);

            // Step 1: npm install (blocking - wait for it to fully finish before starting Vite)
            execCommand(podName, "runner", "sh", "-c", "npm install --no-audit --no-fund --prefer-offline");
            // Step 2: Ensure port 5173 is free (kill by port, not by process name)
            execCommand(podName, "runner", "sh", "-c", "fuser -k 5173/tcp 2>/dev/null || pkill -9 -f vite 2>/dev/null || true; sleep 1");
            // Step 3: Start Vite in background
            execCommand(podName, "runner", "sh", "-c", "nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &");

            Pod updatedPod = client.pods().inNamespace(namespace).withName(podName).get();
            registerRoute(domain, updatedPod);

            log.info("Deployment successful: {}", formattedUrl);
            return new DeployResponse(formattedUrl);

        } catch (Exception e) {
            log.error("Deployment failed for project {}. Releasing pod {}.", projectId, podName, e);
            client.pods().inNamespace(namespace).withName(podName).delete();
            throw new RuntimeException("Failed to deploy project " + projectId + ": " + e.getMessage(), e);
        }
    }

    private void registerRoute(String domain, Pod pod) {
        String podIp = pod.getStatus().getPodIP();
        if (podIp == null) throw new RuntimeException("Pod is running but has no IP!");
/** 
        [OPTIMIZED]: Reduced TTL from 6 hours to 30 minutes.
        Previously 6h was set to keep a preview alive for a long coding session.
        For a showcase project with 1 runner pod, a stale route means the pod is burning
        100m CPU + 256Mi RAM indefinitely with no user connected.
        30 minutes is generous for a demo session; the user can always re-open the project.
        The cleanupStalePods() scheduler below then recycles the pod within the next hour check.
        To restore: change 30L, TimeUnit.MINUTES back to 6L, TimeUnit.HOURS
        redisTemplate.opsForValue().set("route:" + domain, podIp + ":5173", 6, TimeUnit.HOURS);  // old: 6h TTL */
        redisTemplate.opsForValue().set("route:" + domain, podIp + ":5173", 30, TimeUnit.MINUTES);
        log.info("Route Registered: {} -> {}", domain, podIp);
    }

    /**
     * [NEW] Auto-release stale pods.
     *
     * Runs every 30 minutes. Finds all busy pods whose Redis route has expired
     * (meaning no user has been actively using the preview for at least 30 minutes).
     * Releases those pods back to idle so the runner pool is freed up.
     *
     * Without this, a pod claimed at 9am would keep Vite running and hold 256Mi RAM
     * until another project explicitly evicts it — which may never happen in a showcase.
     *
     * To disable: remove @Scheduled and @EnableScheduling from WorkspaceServiceApplication.
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void cleanupStalePods() {
        log.info("[Scheduler] Running stale pod cleanup...");
        List<Pod> busyPods = client.pods().inNamespace(namespace)
                .withLabel(POOL_LABEL, BUSY)
                .list().getItems();

        for (Pod pod : busyPods) {
            String projectIdStr = pod.getMetadata().getLabels().get(PROJECT_LABEL);
            if (projectIdStr == null) continue;

            String domain = "project-" + projectIdStr + "." + baseDomain;
            Boolean routeExists = redisTemplate.hasKey("route:" + domain);

            if (Boolean.FALSE.equals(routeExists)) {
                log.info("[Scheduler] Route expired for project {}. Releasing pod {}.",
                        projectIdStr, pod.getMetadata().getName());
                try {
                    release(Long.parseLong(projectIdStr));
                } catch (Exception e) {
                    log.error("[Scheduler] Failed to release pod for project {}: {}", projectIdStr, e.getMessage());
                }
            }
        }
        log.info("[Scheduler] Stale pod cleanup complete.");
    }

    private void execCommand(String podName, String container, String... command) {
        log.debug("Exec in {}:{} -> {}", podName, container, String.join(" ", command));

        CompletableFuture<String> data = new CompletableFuture<>();
        try (ExecWatch ignored = client.pods().inNamespace(namespace).withName(podName)
                .inContainer(container)
                .writingOutput(new ByteArrayOutputStream())
                .writingError(new ByteArrayOutputStream())
                .usingListener(new ExecListener() {
                    @Override
                    public void onClose(int code, String reason) {
                        data.complete("Done");
                    }
                })
                .exec(command)) {

            if (command[command.length - 1].trim().endsWith("&")) {
                Thread.sleep(500);
            } else {
                // [OPTIMIZED]: Increased timeout to 5 minutes.
                // This is a maximum deadline, not a sleep. If the command (like 'npm install')
                // completes in 40 seconds, it returns immediately. This prevents GKE cold-start timeouts.
                data.get(5, TimeUnit.MINUTES);
            }

        } catch (Exception e) {
            log.error("Exec failed", e);
            throw new RuntimeException("Pod Execution Failed", e);
        }
    }
    private void killExistingWatchers(String podName) {
        execCommand(podName, "syncer", "sh", "-c", "pkill -f 'mc mirror' || true");
    }

    @Override
    public void release(Long projectId) {
        Pod pod = findClaimedPod(projectId);
        if (pod == null) return;

        String podName = pod.getMetadata().getName();
        String phase = pod.getStatus().getPhase();
        if ("Running".equals(phase)) {
            killExistingWatchers(podName);
            execCommand(podName, "runner", "sh", "-c", "fuser -k 5173/tcp 2>/dev/null || pkill -9 -f vite 2>/dev/null || true; for f in /app/* /app/.[!.]*; do [ -e \"$f\" ] && [ \"$f\" != \"/app/node_modules\" ] && rm -rf \"$f\"; done");
        }

        client.pods().inNamespace(namespace).withName(podName).edit(p -> {
            p.getMetadata().getLabels().put(POOL_LABEL, IDLE);
            p.getMetadata().getLabels().remove(PROJECT_LABEL);
            return p;
        });

        log.info("Released pod {} back to idle pool", podName);
    }

    @Override
    public DeploymentLogsResponse getDeploymentLogs(Long projectId) {
        Pod pod = findClaimedPod(projectId);
        if (pod == null) {
            log.info("No claimed pod found for project {} during log request. Triggering automatic deployment...", projectId);
            try {
                deploy(projectId, false);
                return new DeploymentLogsResponse(projectId, "STARTING", "No active pod was found. Automatically started a new deployment runner. Please poll again.");
            } catch (Exception e) {
                return new DeploymentLogsResponse(projectId, "UNREACHABLE", "No active pod found and auto-deploy failed: " + e.getMessage());
            }
        }

        String phase = pod.getStatus().getPhase();
        if (!"Running".equals(phase)) {
            return new DeploymentLogsResponse(projectId, "STARTING", "Pod is currently booting up (Status: " + phase + "). Please wait...");
        }

        String podName = pod.getMetadata().getName();
        String logs = execCommandWithOutput(podName, "runner", "cat", "/app/dev.log");
        
        boolean isViteRunning = isProcessRunning(podName, "vite");
        boolean isNpmInstallRunning = isProcessRunning(podName, "npm install");
        
        String status = "STARTING";
        if (logs.contains("ready in") || logs.contains("Local:") || logs.contains("Network:")) {
            status = "RUNNING";
        } else if (isViteRunning || isNpmInstallRunning) {
            status = "STARTING";
        } else {
            status = "CRASHED";
        }
        
        return new DeploymentLogsResponse(projectId, status, logs);
    }

    private boolean isProcessRunning(String podName, String processName) {
        String output = execCommandWithOutput(podName, "runner", "sh", "-c", "ps aux");
        return output != null && output.contains(processName);
    }

    private String execCommandWithOutput(String podName, String container, String... command) {
        log.debug("Exec with output in {}:{} -> {}", podName, container, String.join(" ", command));

        CompletableFuture<String> data = new CompletableFuture<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (ExecWatch ignored = client.pods().inNamespace(namespace).withName(podName)
                .inContainer(container)
                .writingOutput(out)
                .writingError(err)
                .usingListener(new ExecListener() {
                    @Override
                    public void onClose(int code, String reason) {
                        data.complete("Done");
                    }
                })
                .exec(command)) {

            data.get(10, TimeUnit.SECONDS);
            return out.toString();

        } catch (Exception e) {
            log.error("Exec with output failed", e);
            return "Failed to fetch logs: " + e.getMessage() + "\nStderr: " + err.toString();
        }
    }
}
