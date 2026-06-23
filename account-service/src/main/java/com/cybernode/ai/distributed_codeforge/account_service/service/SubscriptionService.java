package com.cybernode.ai.distributed_codeforge.account_service.service;



import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.SubscriptionResponse;
import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;

import java.time.Instant;

public interface SubscriptionService {
    SubscriptionResponse getCurrentSubscription();

    void activateSubscription(Long userID, Long planID, String subscriptionID,
                              String customerId, Instant periodStart, Instant periodEnd);

    void updateSubscription(String subscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId);

    void cancelSubscription(String subscriptionId);

    void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd);

    void markSubscriptionPastDue(String subId);

    PlanDto getCurrentSubscribedPlanByUser();
}
