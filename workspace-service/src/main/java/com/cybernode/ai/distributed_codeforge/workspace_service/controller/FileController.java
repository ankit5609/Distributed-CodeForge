package com.cybernode.ai.distributed_codeforge.workspace_service.controller;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectFileService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
@Validated
public class FileController {
    private final ProjectFileService fileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable @NotNull @Min(1) Long projectId){
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFile(@PathVariable @NotNull @Min(1) Long projectId,
                                           @RequestParam @NotBlank String path){
        return ResponseEntity.ok(fileService.getFileContent(projectId,path));
    }

    @GetMapping("/attachments/{fileName}")
    public ResponseEntity<byte[]> getChatAttachment(
            @PathVariable @NotNull @Min(1) Long projectId,
            @PathVariable @NotBlank String fileName) {
        byte[] data = fileService.getAttachment(projectId, fileName);
        String contentType = java.net.URLConnection.guessContentTypeFromName(fileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
