package com.cybernode.ai.distributed_codeforge.workspace_service.service;

import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.FileContentResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.FileTreeResponse;

public interface ProjectFileService {
    FileTreeResponse getFileTree(Long projectId);

    FileContentResponse getFileContent(Long projectId, String path);

    void saveFile(String filePath, String fileContent, Long projectId);
}
