package com.cybernode.ai.distributed_codeforge.workspace_service.mapper;

import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectRole;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.MemberResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.ProjectResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.project.ProjectSummaryResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.Project;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectResponse toProjectResponse(Project project);

    List<ProjectSummaryResponse> toListOfProjectSummaryResponse(List<Project> projects);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "role", source = "projectRole")
    MemberResponse toMemberResponseFromProjectMember(ProjectMember projectMember);

    ProjectSummaryResponse toProjectSummaryResponse(Project project, ProjectRole role);
}
