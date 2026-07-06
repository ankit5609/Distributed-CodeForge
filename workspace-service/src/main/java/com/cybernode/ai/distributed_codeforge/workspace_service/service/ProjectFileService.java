package com.cybernode.ai.distributed_codeforge.workspace_service.service;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;

import org.springframework.web.multipart.MultipartFile;

public interface ProjectFileService {
    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFile(String filePath, String fileContent, Long projectId);

    String uploadAttachment(Long projectId, MultipartFile file);

    byte[] getAttachment(Long projectId, String fileName);
}
