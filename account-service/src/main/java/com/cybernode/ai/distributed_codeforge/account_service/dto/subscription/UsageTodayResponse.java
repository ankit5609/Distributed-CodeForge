package com.cybernode.ai.distributed_codeforge.account_service.dto.subscription;

public record UsageTodayResponse(
        Integer tokensUsed,
        Integer tokenLimit,
        Integer previousRunning,
        Integer previewsLimit
) {
}
