package com.cybernode.ai.distributed_codeforge.account_service.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank String username,
        @Size(min = 4 , max=50) String password
) {
}
