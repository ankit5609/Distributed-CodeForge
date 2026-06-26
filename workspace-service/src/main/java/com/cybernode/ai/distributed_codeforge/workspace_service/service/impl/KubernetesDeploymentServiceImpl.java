package com.cybernode.ai.distributed_codeforge.workspace_service.service.impl;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeployResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.DeploymentService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

    public DeployResponse deploy(Long projectId) {
        // Dynamically build the domain: project-123.app.domain.com
        String domain = "project-" + projectId + "." + baseDomain;

        // Use default port 80 format logic for clean URLs, or explicit ports for local testing
        String formattedUrl = proxyPort.equals("80")
                ? "http://" + domain
                : "http://" + domain + ":" + proxyPort;

        Pod existingPod = findActivePod(projectId);

        if (existingPod != null) {
            String podName = existingPod.getMetadata().getName();
            log.info("Found existing pod {} for project {}. Resuming and updating server...", podName, projectId);
            registerRoute(domain, existingPod);
            
            try {
                // Run npm install to pick up any new dependencies (e.g. framer-motion) and restart Vite
                String startCmd = "npm install && pkill -f 'npm run dev' || true && nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";
                execCommand(podName, "runner", "sh", "-c", startCmd);
            } catch (Exception e) {
                log.warn("Failed to restart dev server on existing pod {}, attempting clean redeploy...", podName, e);
                client.pods().inNamespace(namespace).withName(podName).delete();
                return claimAndStartNewPod(projectId, domain, formattedUrl);
            }
            
            return new DeployResponse(formattedUrl);
        }

        return claimAndStartNewPod(projectId, domain, formattedUrl);
    }

    private Pod findActivePod(Long projectId) {
        return client.pods().inNamespace(namespace)
                .withLabel(PROJECT_LABEL, projectId.toString())
                .withLabel(POOL_LABEL, BUSY)
                .list().getItems().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
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
            String initialSyncCmd = String.format("rm -rf /app/* && mc mirror --overwrite myminio/projects/%d/ /app/", projectId);
            execCommand(podName, "syncer", "sh", "-c", initialSyncCmd);

            String watchCmd = String.format("nohup mc mirror --overwrite --watch myminio/projects/%d/ /app/ > /app/sync.log 2>&1 &", projectId);
            execCommand(podName, "syncer", "sh", "-c", watchCmd);

            String startCmd = "npm install && nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";
            execCommand(podName, "runner", "sh", "-c", startCmd);

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

        redisTemplate.opsForValue().set("route:" + domain, podIp + ":5173", 6, TimeUnit.HOURS);
        log.info("Route Registered: {} -> {}", domain, podIp);
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
                data.get(30, TimeUnit.SECONDS);
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
        Pod pod = findActivePod(projectId);
        if (pod == null) return;

        String podName = pod.getMetadata().getName();
        killExistingWatchers(podName);
        execCommand(podName, "runner", "sh", "-c", "pkill -f 'npm run dev' || true; rm -rf /app/*");

        client.pods().inNamespace(namespace).withName(podName).edit(p -> {
            p.getMetadata().getLabels().put(POOL_LABEL, IDLE);
            p.getMetadata().getLabels().remove(PROJECT_LABEL);
            return p;
        });

        log.info("Released pod {} back to idle pool", podName);
    }



}
