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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.billing.mode:LOCAL}")
    private String billingMode;

    private final Integer FREE_TIER_PROJECTS_ALLOWED=0;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId=authUtil.getCurrentUserId();
        var currentSubscription= subscriptionRepository.findByUserIdAndStatusIn(userId, Set.of(
                SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE,
                SubscriptionStatus.TRIALING, SubscriptionStatus.DEMO_LOCKED
        )).orElse(null);
        if (currentSubscription == null) {
            return new SubscriptionResponse(null, "NONE", null, null, null);
        }

        SubscriptionResponse response = planSubscriptionMapper.toSubscriptionResponse(currentSubscription);
        
        // If the subscription is DEMO_LOCKED, return a custom warning message for the UI
        if (currentSubscription.getStatus() == SubscriptionStatus.DEMO_LOCKED) {
            String warningMsg = "Your payment was successfully processed using Stripe Test Mode. " +
                    "This project is intended for learning and demonstration purposes. " +
                    "Although the payment flow completed successfully, premium resources remain unavailable " +
                    "because no real payment was processed.";
            return new SubscriptionResponse(
                    response.plan(),
                    response.status(),
                    response.currentPeriodEnd(),
                    response.tokensUsedThisCycle(),
                    warningMsg
            );
        }

        return response;
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {

        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) return;

        User user = getUser(userId);
        Plan plan = getPlan(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.INCOMPLETE)
                .build();

        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        boolean hasSubscriptionUpdated=false;
        
        SubscriptionStatus finalStatus = adjustStatusForDemoMode(status);
        if(finalStatus!=null && finalStatus!=subscription.getStatus()){
            subscription.setStatus(finalStatus);
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

        Optional<Subscription> optional = Optional.empty();
        for (int i = 0; i < 5; i++) {
            optional = subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId);
            if (optional.isPresent()) {
                break;
            }
            try {
                log.info("Subscription {} not found in DB yet. Retry attempt {}/5 in 500ms...", gatewaySubscriptionId, i + 1);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry wait interrupted", e);
            }
        }

        if (optional.isEmpty()) {
            log.warn("Subscription {} not found in renewSubscriptionPeriod after retries, skipping.", gatewaySubscriptionId);
            return;
        }
        Subscription subscription = optional.get();
        Instant newStart=periodStart!=null ? periodStart:subscription.getCurrentPeriodStart();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if(subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE){
            subscription.setStatus(adjustStatusForDemoMode(SubscriptionStatus.ACTIVE));
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
        if (subscriptionResponse == null || "NONE".equals(subscriptionResponse.status()) || "DEMO_LOCKED".equals(subscriptionResponse.status())) {
            // [DEMO MODE / NO ACTIVE SUB]: Enforce Free plan limits for workspace-service and intelligence-service
            return new PlanDto(null, "FREE", FREE_TIER_PROJECTS_ALLOWED, 5000, false, "0");
        }
        return subscriptionResponse.plan();
    }


    /// Utility methods

    private SubscriptionStatus adjustStatusForDemoMode(SubscriptionStatus status) {
        if ("DEMO".equalsIgnoreCase(billingMode)) {
            if (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING) {
                return SubscriptionStatus.DEMO_LOCKED;
            }
        }
        return status;
    }

    private User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User",userId.toString()));
    }

    private Plan getPlan(Long planId){
        return planRepository.findById(planId).orElseThrow(()-> new ResourceNotFoundException("Plan",planId.toString()));
    }
    private Subscription getSubscription(String gatewaySubscriptionId) {
        Optional<Subscription> optional = Optional.empty();
        for (int i = 0; i < 5; i++) {
            optional = subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId);
            if (optional.isPresent()) {
                break;
            }
            try {
                log.info("Subscription {} not found in DB yet (helper). Retry attempt {}/5 in 500ms...", gatewaySubscriptionId, i + 1);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry wait interrupted", e);
            }
        }
        return optional.orElseThrow(() -> new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }

}
