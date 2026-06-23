package com.cybernode.ai.distributed_codeforge.workspace_service.dto.member;



import com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String username,
        String name,
        ProjectRole role,
        Instant invitedAt
) {
}
