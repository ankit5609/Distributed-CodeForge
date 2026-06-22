package com.cybernode.ai.distributed_codeforge.account_service.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @Size(min=1, max=30) String name,
        @NotBlank String username,
        @Size(min=4,max=50) String password
) {
}
