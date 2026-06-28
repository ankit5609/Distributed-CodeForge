package com.cybernode.ai.distributed_codeforge.intelligence_service.llm.tools;

import com.cybernode.ai.distributed_codeforge.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CodeGenerationTools {

    private final Long projectId;
    private final WorkspaceClient workspaceClient;

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
}
