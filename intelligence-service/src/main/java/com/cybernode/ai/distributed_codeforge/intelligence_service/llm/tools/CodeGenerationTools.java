package com.cybernode.ai.distributed_codeforge.intelligence_service.llm.tools;

import com.cybernode.ai.distributed_codeforge.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CodeGenerationTools {

    private final Long projectId;
    private final WorkspaceClient workspaceClient;
    private final VectorStore vectorStore;

    // Temporary failure context to track fix loops in the active turn
    private String lastFailingLogs;
    private Map<String, String> filesStateAtFailure;

    @Tool(name = "read_files",
            description = "Read the content of files. Only input the file names present inside the FILE_TREE. DO NOT input any path which is not present under the FILE_TREE.")
    public List<String> readFiles(
            @ToolParam(description = "List of relative paths (e.g., ['src/App.tsx'])")
            List<String> paths){
        List<String> result=new ArrayList<>();
        for(String path:paths){
            String cleanPath=path.startsWith("/")?path.substring(1):path;
            String content=workspaceClient.getFileContent(projectId,cleanPath);

            result.add(String.format(
                    "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---",cleanPath,content
            ));
        }
        return result;
    }

    @Tool(name = "deploy_and_verify_preview",
            description = "Triggers kubernetes deployment for this project and polls runtime logs. Call this after editing files to verify the build compiles cleanly before returning.")
    public String deployAndVerifyPreview() {
        try {
            workspaceClient.deployProject(projectId, true);
        } catch (Exception e) {
            return "FAILED: Failed to trigger deployment: " + e.getMessage();
        }

        String statusResult = pollLogs();
        
        if (statusResult.startsWith("SUCCESS:")) {
            // If we successfully fixed a crash in this turn, save the fix pair to vectorStore
            if (lastFailingLogs != null) {
                try {
                    Map<String, String> filesAfter = snapshotFiles();
                    String fixDescription = computeDiff(filesStateAtFailure, filesAfter);
                    
                    if (!fixDescription.trim().isEmpty()) {
                        String docId = UUID.randomUUID().toString();
                        Document doc = new Document(
                                docId,
                                lastFailingLogs, // searchable content is the compilation error logs
                                Map.of(
                                        "type", "error_fix",
                                        "fix", fixDescription
                                )
                        );
                        vectorStore.add(List.of(doc));
                        log.info("Successfully recorded build fix memory in pgvector. DocId: {}", docId);
                    }
                } catch (Exception e) {
                    log.error("Failed to save build fix to vector store", e);
                } finally {
                    // Clear state to avoid leaking to next failure
                    lastFailingLogs = null;
                    filesStateAtFailure = null;
                }
            }
            return statusResult;
            
        } else if (statusResult.startsWith("FAILED:")) {
            String errorLogs = statusResult.substring(7).trim(); // strip the "FAILED:" prefix for similarity search
            
            // Search vector store for a similar past fix
            String hintMessage = "";
            try {
                List<Document> similarFixes = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(errorLogs)
                                .filterExpression("type == 'error_fix'")
                                .topK(1)
                                .similarityThreshold(0.7) // match highly similar build errors
                                .build()
                );
                
                if (similarFixes != null && !similarFixes.isEmpty()) {
                    Document matchedDoc = similarFixes.get(0);
                    String pastFix = matchedDoc.getMetadata().getOrDefault("fix", "").toString();
                    if (!pastFix.trim().isEmpty()) {
                        // Clearly separate the current error from the hint so the AI does not get confused
                        hintMessage = "\n\n=========================================\n" +
                                      "HINT: A similar build error was solved in a past run.\n" +
                                      "Here is the diff that resolved it previously:\n\n" +
                                      pastFix + "\n" +
                                      "=========================================";
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query similar past build fixes", e);
            }
            
            // Capture the failure state for the active turn so we can diff when it is successfully fixed
            lastFailingLogs = errorLogs;
            filesStateAtFailure = snapshotFiles();
            
            return statusResult + hintMessage;
        }
        
        return statusResult;
    }

    // Helper method to poll logs from GKE
    private String pollLogs() {
        for (int i = 0; i < 15; i++) {
            try {
                var response = workspaceClient.getDeploymentLogs(projectId);
                if (response != null) {
                    if ("RUNNING".equals(response.getStatus())) {
                        return "SUCCESS: Vite development server is ready on http://project-" + projectId + ".previews.codeforge.arclite.site";
                    }
                    if ("CRASHED".equals(response.getStatus())) {
                        return "FAILED: Compilation or start error:\n" + response.getLogs();
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            var finalResponse = workspaceClient.getDeploymentLogs(projectId);
            if (finalResponse != null) {
                return "TIMEOUT: Vite server is taking too long to start. Current logs:\n" + finalResponse.getLogs();
            }
        } catch (Exception ignored) {
        }
        return "TIMEOUT: Vite server is taking too long to start.";
    }

    // Fetch a snapshot of all file paths and contents in the workspace
    private Map<String, String> snapshotFiles() {
        Map<String, String> snapshot = new HashMap<>();
        try {
            var tree = workspaceClient.getFileTree(projectId);
            if (tree != null && tree.files() != null) {
                for (var node : tree.files()) {
                    String content = workspaceClient.getFileContent(projectId, node.path());
                    snapshot.put(node.path(), content != null ? content : "");
                }
            }
        } catch (Exception e) {
            log.error("Failed to snapshot workspace files for project: {}", projectId, e);
        }
        return snapshot;
    }

    // Compute simple file modifications between failure and success states
    private String computeDiff(Map<String, String> before, Map<String, String> after) {
        StringBuilder diff = new StringBuilder();
        for (var entry : after.entrySet()) {
            String path = entry.getKey();
            String contentAfter = entry.getValue();
            if (!before.containsKey(path)) {
                diff.append("Added file: ").append(path).append("\n")
                    .append("New Content:\n").append(contentAfter).append("\n\n");
            } else {
                String contentBefore = before.get(path);
                if (!contentBefore.equals(contentAfter)) {
                    diff.append("Modified file: ").append(path).append("\n")
                        .append("New Content:\n").append(contentAfter).append("\n\n");
                }
            }
        }
        for (String path : before.keySet()) {
            if (!after.containsKey(path)) {
                diff.append("Deleted file: ").append(path).append("\n\n");
            }
        }
        return diff.toString();
    }
}
