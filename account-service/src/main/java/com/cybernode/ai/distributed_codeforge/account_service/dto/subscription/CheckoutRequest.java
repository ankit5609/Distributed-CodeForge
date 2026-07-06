package com.cybernode.ai.distributed_codeforge.account_service.dto.subscription;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Plan ID is required") Long planId
) {
}
