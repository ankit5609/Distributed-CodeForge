package com.cybernode.ai.distributed_codeforge.account_service.dto.auth;

public record UserProfileResponse(
        Long id,
        String username,
        String name
) {
}
