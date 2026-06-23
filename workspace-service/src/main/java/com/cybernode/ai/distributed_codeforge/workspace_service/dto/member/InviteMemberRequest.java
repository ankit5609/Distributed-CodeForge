package com.cybernode.ai.distributed_codeforge.workspace_service.dto.member;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank String username,
        @NotNull ProjectRole role
) {
}
