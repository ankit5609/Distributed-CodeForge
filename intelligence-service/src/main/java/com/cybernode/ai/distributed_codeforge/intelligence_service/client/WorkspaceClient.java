package com.cybernode.ai.distributed_codeforge.intelligence_service.client;


import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectPermission;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.DeploymentLogsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "workspace-service", path = "/workspace",url="${WORKSPACE_SERVICE_URI:}")
public interface WorkspaceClient {

    @GetMapping("/internal/v1/projects/{projectId}/files/tree")
    FileTreeDto getFileTree(@PathVariable("projectId") Long projectId);

    @GetMapping("/internal/v1/projects/{projectId}/files/content")
    String getFileContent(@PathVariable("projectId") Long projectId, @RequestParam("path") String path);

    @GetMapping("/internal/v1/projects/{projectId}/permissions/check")
    boolean checkPermission(
            @PathVariable("projectId") Long projectId,
            @RequestParam("permission") ProjectPermission permission);

    @PostMapping("/projects/{projectId}/deploy")
    Object deployProject(@PathVariable("projectId") Long projectId, @RequestParam("force") boolean force);

    @GetMapping("/projects/{projectId}/logs")
    DeploymentLogsResponse getDeploymentLogs(@PathVariable("projectId") Long projectId);

    @PostMapping(value = "/internal/v1/projects/{projectId}/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadChatAttachment(@PathVariable("projectId") Long projectId, @RequestPart("file") MultipartFile file);

    @GetMapping(value = "/internal/v1/projects/{projectId}/attachments/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] getChatAttachment(@PathVariable("projectId") Long projectId, @PathVariable("fileName") String fileName);
}
