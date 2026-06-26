package com.cybernode.ai.distributed_codeforge.workspace_service.service.impl;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.UserDto;
import com.cybernode.ai.distributed_codeforge.common_lib.error.ResourceNotFoundException;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.workspace_service.client.AccountClient;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.InviteMemberRequest;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.MemberResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.Project;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.ProjectMember;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.ProjectMemberId;
import com.cybernode.ai.distributed_codeforge.workspace_service.mapper.ProjectMapper;
import com.cybernode.ai.distributed_codeforge.workspace_service.repository.ProjectMemberRepository;
import com.cybernode.ai.distributed_codeforge.workspace_service.repository.ProjectRepository;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    AuthUtil authUtil;
    AccountClient accountClient;

    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMapper::toMemberResponseFromProjectMember)
                .toList();
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId=authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);

        UserDto invitee = accountClient.getUserByEmail(request.username()).orElseThrow(
                () -> new ResourceNotFoundException("User", request.username())
        );
        if(invitee.id().equals(userId)){
            throw new RuntimeException("Cannot invite yourself");
        }
        ProjectMemberId projectMemberId=new ProjectMemberId(invitee.id(), projectId);

        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Cannot invite once again");
        }

        ProjectMember member= ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(request.role())
                .project(project)
                .invitedAt(Instant.now())
                .build();
        projectMemberRepository.save(member);

        return projectMapper.toMemberResponseFromProjectMember(member);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId=authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);


        if(request.role().toString().equals("OWNER")){
            throw new RuntimeException("Invalid role Update");
        }

        ProjectMemberId projectMemberId=new ProjectMemberId(memberId, projectId);
        ProjectMember projectMember=projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());
        projectMemberRepository.save(projectMember);
        return projectMapper.toMemberResponseFromProjectMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {

        Long userId=authUtil.getCurrentUserId();

        Project project = getAccessibleProjectById(projectId, userId);

        ProjectMemberId projectMemberId=new ProjectMemberId(memberId, projectId);

        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Invalid member");
        }

        projectMemberRepository.deleteById(projectMemberId);

        return ;
    }

    /// INTERNAL FUNCTIONS
    /// Give project if this user is allowed to access this project
    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();
    }
}
