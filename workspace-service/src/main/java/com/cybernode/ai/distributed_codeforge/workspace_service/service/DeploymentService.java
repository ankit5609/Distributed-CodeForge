package com.cybernode.ai.distributed_codeforge.workspace_service.service;


import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeployResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId);
    void release(Long projectId);
}
