package com.cybernode.ai.distributed_codeforge.workspace_service.service;


import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeployResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.DeploymentLogsResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId, boolean force);
    void release(Long projectId);
    DeploymentLogsResponse getDeploymentLogs(Long projectId);
}
