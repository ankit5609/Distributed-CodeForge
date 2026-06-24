package com.cybernode.ai.distributed_codeforge.workspace_service.controller;


import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectPermission;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectFileService;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/internal/v1/")
@RestController
public class InternalWorkspaceController {

    private final ProjectService projectService;
    private final ProjectFileService projectFileService;

    @GetMapping("/projects/{projectId}/files/tree")
    public FileTreeDto getFileTree(@PathVariable Long projectId) {
        return projectFileService.getFileTree(projectId);
    }

    @GetMapping("/projects/{projectId}/files/content")
    public String getFileContent(@PathVariable Long projectId, @RequestParam String path) {
        return projectFileService.getFileContent(projectId, path);
    }

    @GetMapping("/projects/{projectId}/permissions/check")
    public boolean checkProjectPermission(
            @PathVariable Long projectId,
            @RequestParam ProjectPermission permission) {
        return projectService.hasPermission(projectId, permission);
    }
}
