package com.cybernode.ai.distributed_codeforge.account_service.dto.auth;

public record  AuthResponse (
        String token,
        UserProfileResponse user
){
}
