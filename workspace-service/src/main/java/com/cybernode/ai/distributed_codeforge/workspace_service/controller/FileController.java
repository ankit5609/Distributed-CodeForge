package com.cybernode.ai.distributed_codeforge.workspace_service.controller;


import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/files")
public class FileController {
    private final ProjectFileService fileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId){
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFile(@PathVariable Long projectId,
                                                       @RequestParam String path){
        return ResponseEntity.ok(fileService.getFileContent(projectId,path));
    }
}
