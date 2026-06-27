package com.cybernode.ai.distributed_codeforge.common_lib.event;

import java.time.Instant;

public record SubscriptionEvent(
        String eventType,
        Long userId,
        Long planId,
        String subscriptionId,
        String customerId,
        String status,
        Instant periodStart,
        Instant periodEnd,
        Boolean cancelAtPeriodEnd
) {}
