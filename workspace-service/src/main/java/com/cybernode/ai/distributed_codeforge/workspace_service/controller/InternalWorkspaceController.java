package com.cybernode.ai.distributed_codeforge.workspace_service.controller;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectPermission;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectFileService;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RequestMapping("/internal/v1/")
@RestController
@Validated
public class InternalWorkspaceController {

    private final ProjectService projectService;
    private final ProjectFileService projectFileService;

    @GetMapping("/projects/{projectId}/files/tree")
    public FileTreeDto getFileTree(@PathVariable @NotNull @Min(1) Long projectId) {
        return projectFileService.getFileTree(projectId);
    }

    @GetMapping("/projects/{projectId}/files/content")
    public String getFileContent(@PathVariable @NotNull @Min(1) Long projectId, @RequestParam @NotBlank String path) {
        return projectFileService.getFileContent(projectId, path);
    }

    @GetMapping("/projects/{projectId}/permissions/check")
    public boolean checkProjectPermission(
            @PathVariable @NotNull @Min(1) Long projectId,
            @RequestParam @NotNull ProjectPermission permission) {
        return projectService.hasPermission(projectId, permission);
    }

    @PostMapping(value = "/projects/{projectId}/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadChatAttachment(
            @PathVariable @NotNull @Min(1) Long projectId,
            @RequestPart("file") MultipartFile file) {
        return projectFileService.uploadAttachment(projectId, file);
    }

    @GetMapping("/projects/{projectId}/attachments/{fileName}")
    public byte[] getChatAttachment(
            @PathVariable @NotNull @Min(1) Long projectId,
            @PathVariable @NotBlank String fileName) {
        return projectFileService.getAttachment(projectId, fileName);
    }
}
