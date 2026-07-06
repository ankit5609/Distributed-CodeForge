package com.cybernode.ai.distributed_codeforge.workspace_service.controller;

import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.InviteMemberRequest;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.MemberResponse;
import com.cybernode.ai.distributed_codeforge.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.cybernode.ai.distributed_codeforge.workspace_service.service.ProjectMemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/members")
@RequiredArgsConstructor
@Validated
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMembers(@PathVariable @NotNull @Min(1) Long projectId){

        return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(@PathVariable @NotNull @Min(1) Long projectId,
                                                       @RequestBody @Valid InviteMemberRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(
                projectMemberService.inviteMember(projectId,request)
        );
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable @NotNull @Min(1) Long projectId,
            @PathVariable @NotNull @Min(1) Long memberId,
            @RequestBody @Valid UpdateMemberRoleRequest request){

        return ResponseEntity.ok(projectMemberService.updateMemberRole(projectId,memberId,request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMemberRole(
            @PathVariable @NotNull @Min(1) Long projectId,
            @PathVariable @NotNull @Min(1) Long memberId){

        projectMemberService.removeProjectMember(projectId,memberId);
        return ResponseEntity.noContent().build();
    }

}
