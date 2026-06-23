package com.cybernode.ai.distributed_codeforge.account_service.service.impl;


import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.SubscriptionResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Plan;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Subscription;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.account_service.mapper.PlanSubscriptionMapper;
import com.cybernode.ai.distributed_codeforge.account_service.repository.PlanRepository;
import com.cybernode.ai.distributed_codeforge.account_service.repository.SubscriptionRepository;
import com.cybernode.ai.distributed_codeforge.account_service.repository.UserRepository;
import com.cybernode.ai.distributed_codeforge.account_service.service.SubscriptionService;
import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;
import com.cybernode.ai.distributed_codeforge.common_lib.error.ResourceNotFoundException;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanSubscriptionMapper planSubscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    private final Integer FREE_TIER_PROJECTS_ALLOWED=100;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId=authUtil.getCurrentUserId();
        var currentSubscription= subscriptionRepository.findByUserIdAndStatusIn(userId, Set.of(
                SubscriptionStatus.ACTIVE,SubscriptionStatus.PAST_DUE,
                SubscriptionStatus.TRIALING
        )).orElse(null);
        if (currentSubscription == null) {
            return new SubscriptionResponse(null, "NONE", null, null);
        }

        return planSubscriptionMapper.toSubscriptionResponse(currentSubscription);
    }

    @Override
    public void activateSubscription(Long userID, Long planID, String subscriptionID,
                                     String customerId, Instant periodStart, Instant periodEnd) {
        boolean exists=subscriptionRepository.existsByStripeSubscriptionId(subscriptionID);
        if(exists) return;

        User user=getUser(userID);
        Plan plan=getPlan(planID);

        Subscription subscription=Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionID)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);

    }

    @Override
    @Transactional
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        boolean hasSubscriptionUpdated=false;
        if(status!=null && status!=subscription.getStatus()){
            subscription.setStatus(status);
            hasSubscriptionUpdated=true;
        }

        if(periodStart!=null && !periodStart.equals(subscription.getCurrentPeriodStart())){
            subscription.setCurrentPeriodStart(periodStart);
            hasSubscriptionUpdated=true;
        }
        if(periodEnd!=null && !periodEnd.equals(subscription.getCurrentPeriodEnd())){
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionUpdated=true;
        }
        if(cancelAtPeriodEnd != null && !cancelAtPeriodEnd.equals(
                subscription.getCancelAtPeriodEnd() != null ? subscription.getCancelAtPeriodEnd() : false)){
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            hasSubscriptionUpdated = true;
        }

        if(planId != null && (subscription.getPlan() == null || !planId.equals(subscription.getPlan().getId()))){
            Plan newPlan=getPlan(planId);
            subscription.setPlan(newPlan);
            hasSubscriptionUpdated=true;
        }
        if(hasSubscriptionUpdated){
            log.debug("Subscription has been updated: {}",gatewaySubscriptionId);
            subscriptionRepository.save(subscription);
        }

    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);

    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {

        Optional<Subscription> optional = subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId);
        if (optional.isEmpty()) {
            log.warn("Subscription {} not found in renewSubscriptionPeriod, skipping — checkout.session.completed will handle it", gatewaySubscriptionId);
            return;
        }
        Subscription subscription = optional.get();
        Instant newStart=periodStart!=null ? periodStart:subscription.getCurrentPeriodStart();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if(subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE){
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        log.info("sub={}", gatewaySubscriptionId);

        log.info(
                "before {} {}",
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        );

        log.info(
                "incoming {} {}",
                periodStart,
                periodEnd
        );

        subscriptionRepository.save(subscription);
    }

    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        if(subscription.getStatus()==SubscriptionStatus.PAST_DUE){
            log.debug("Subscription is already past due, gatewaySubscriptionId: {}",gatewaySubscriptionId);
            return;
        }
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        //Notify user via email.....
    }

    @Override
    public PlanDto getCurrentSubscribedPlanByUser() {
        SubscriptionResponse subscriptionResponse = getCurrentSubscription();
        return subscriptionResponse.plan();
    }


    /// Utility methods

    private User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User",userId.toString()));
    }

    private Plan getPlan(Long planId){
        return planRepository.findById(planId).orElseThrow(()-> new ResourceNotFoundException("Plan",planId.toString()));
    }
    private Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }

}
