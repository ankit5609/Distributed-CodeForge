package com.cybernode.ai.distributed_codeforge.account_service.dto.subscription;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;

import java.time.Instant;

public record SubscriptionResponse(
        PlanDto plan,
        String status,
        Instant currentPeriodEnd,
        Long tokensUsedThisCycle
) {
}
