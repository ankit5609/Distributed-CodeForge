package com.cybernode.ai.distributed_codeforge.account_service.dto.subscription;

public record PlanResponse(
        Long id,

        String name,

        Integer maxProjects,

        Integer maxTokensPerDay,

        Boolean unlimitedAi,

        String price
) {
}
