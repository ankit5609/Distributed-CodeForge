package com.cybernode.ai.distributed_codeforge.workspace_service.service;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileTreeDto;

public interface ProjectFileService {
    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFile(String filePath, String fileContent, Long projectId);
}
