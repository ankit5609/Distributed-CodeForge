package com.cybernode.ai.distributed_codeforge.workspace_service.service;

import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.ProjectRequest;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.ProjectResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects();

    ProjectSummaryResponse getUserProjectById(Long id);


    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(Long id, ProjectRequest request);

    void softdelete(Long id);
}
