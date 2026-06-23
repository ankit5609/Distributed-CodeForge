package com.cybernode.ai.distributed_codeforge.account_service.service.impl;

import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.PortalResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Plan;
import com.cybernode.ai.distributed_codeforge.account_service.repository.PlanRepository;
import com.cybernode.ai.distributed_codeforge.account_service.repository.UserRepository;
import com.cybernode.ai.distributed_codeforge.account_service.service.PaymentProcessor;
import com.cybernode.ai.distributed_codeforge.account_service.service.SubscriptionService;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;
import com.cybernode.ai.distributed_codeforge.common_lib.error.BadRequestException;
import com.cybernode.ai.distributed_codeforge.common_lib.error.ResourceNotFoundException;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {

    private final AuthUtil authUtil;
    private final PlanRepository planRepository;
    private final StripeClient client;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Value("${client.url}")
    private String frontEndUrl;
    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request) {

        //all steps are tied to stripe only
        Long userId= authUtil.getCurrentUserId();
        User user=userRepository.findById(userId).orElseThrow(
                ()-> new ResourceNotFoundException("User",userId.toString())
        );
        Plan plan=planRepository.findById(request.planId()).orElseThrow(
                ()-> new ResourceNotFoundException("Plan", request.planId().toString())
        );

        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPrice(plan.getStripePriceId()).setQuantity(1L).build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSubscriptionData(
                        new SessionCreateParams.SubscriptionData.Builder()
                                .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                        .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                        .build())
                                .build()
                )
                .setSuccessUrl(frontEndUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontEndUrl + "/cancel.html")
                .putMetadata("user_id",userId.toString())
                .putMetadata("plan_id",plan.getId().toString());
        try {
            String stripeCustomerId=user.getStripeCustomerId();
            if(stripeCustomerId==null || stripeCustomerId.isEmpty()){
                params.setCustomerEmail(user.getUsername());
            }
            else{
                params.setCustomer(stripeCustomerId);
            }
            Session session = client.v1().checkout().sessions().create(params.build());
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {
        Long userId=authUtil.getCurrentUserId();
        User user=getUser(userId);
        String stripeCustomerId=user.getStripeCustomerId();
        if(stripeCustomerId==null || stripeCustomerId.isEmpty()){
            throw new BadRequestException("User does not have a Stripe Customer Id, UserId:"+userId);
        }
        try {
            var portalSession = client.v1().billingPortal().sessions().create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontEndUrl)
                            .build()
            );
            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {

        log.info("Handling stripe event: {}",type);

        switch(type){
            case "checkout.session.completed" -> handleCheckOutSessionCompleted((Session) stripeObject,metadata);
            case "customer.subscription.updated" -> handleCustomerSubscriptionUpdated((Subscription) stripeObject);
            case "customer.subscription.deleted" -> handleCustomerSubscriptionDeleted((Subscription) stripeObject);
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) stripeObject);
            default -> log.debug("Ignoring the event : {}",type);
        }
    }


    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subId=extractSubscriptionId(invoice);
        if(subId==null) return;

        subscriptionService.markSubscriptionPastDue(subId);
    }

    private void handleInvoicePaid(Invoice invoice) {
        String subId=extractSubscriptionId(invoice);
        if(subId==null) return;

        try {
            log.info("SUB ID = {}", subId);
            Subscription subscription=client.v1().subscriptions().retrieve(subId);
            var item=subscription.getItems().getData().get(0);

            log.info(
                    "ITEM START={} ITEM END={}",
                    item.getCurrentPeriodStart(),
                    item.getCurrentPeriodEnd()
            );

            Instant periodStart=toInstant(item.getCurrentPeriodStart());
            Instant periodEnd= toInstant(item.getCurrentPeriodEnd());

            log.info(
                    "converted start={} end={}",
                    periodStart,
                    periodEnd
            );

            subscriptionService.renewSubscriptionPeriod(
                    subId,periodStart,periodEnd
            );
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    private void handleCustomerSubscriptionDeleted(Subscription subscription) {
        if(subscription==null){
            log.error("Subscription object was null inside handleCustomerSubscriptionDeleted()");
            return;
        }
        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleCustomerSubscriptionUpdated(Subscription subscription) {
        if(subscription==null){
            log.error("Subscription object was null in handleCustomerSubscriptionUpdated()");
            return;
        }
        SubscriptionStatus status = mapStripeStatusToEnum(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown status '{}' for subscription {}", subscription.getStatus(), subscription.getId());
            return;
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        Long planId = resolvePlanId(item.getPrice());

        subscriptionService.updateSubscription(
                subscription.getId(),status,periodStart,periodEnd,
                subscription.getCancelAtPeriodEnd(),planId
        );
        
    }

    private void handleCheckOutSessionCompleted(Session session, Map<String, String> metadata) {
        if(session==null){
            log.error("Session object was null");
            return;
        }
        Long userID=Long.parseLong(metadata.get("user_id"));
        Long planID=Long.parseLong(metadata.get("plan_id"));
        String subscriptionID=session.getSubscription();
        String customerId=session.getCustomer();
        User user=getUser(userID);
        if(user.getStripeCustomerId()==null){
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        Instant periodStart = null;
        Instant periodEnd = null;
        try {
            Subscription stripeSub = client.v1().subscriptions().retrieve(subscriptionID);
            var item = stripeSub.getItems().getData().get(0);
            periodStart = toInstant(item.getCurrentPeriodStart());
            periodEnd = toInstant(item.getCurrentPeriodEnd());
        } catch (StripeException e) {
            log.warn("Could not fetch subscription period during checkout: {}", e.getMessage());
        }
        subscriptionService.activateSubscription(userID, planID, subscriptionID, customerId, periodStart, periodEnd);

    }
    private User getUser(Long userId) {
        User user=userRepository.findById(userId).orElseThrow(
                ()-> new ResourceNotFoundException("User", userId.toString())
        );
        return user;
    }

    private String extractSubscriptionId(Invoice invoice){
        var parent=invoice.getParent();
        if(parent==null) return null;

        var subDetails=parent.getSubscriptionDetails();
        if(subDetails==null) return null;

        return subDetails.getSubscription();
    }

    private Long resolvePlanId(Price price) {
        if(price ==null || price.getId()==null) return null;
        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
    }

    private Instant toInstant(Long epoch) {
        return epoch!=null ? Instant.ofEpochSecond(epoch):null;
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch (status) {
            case "active" -> SubscriptionStatus.ACTIVE;

            case "trialing" -> SubscriptionStatus.TRIALING;

            case "past_due",
                 "unpaid",
                 "paused",
                 "incomplete_expired" -> SubscriptionStatus.PAST_DUE;

            case "canceled" -> SubscriptionStatus.CANCELED;

            case "incomplete" -> SubscriptionStatus.INCOMPLETE;

            default -> {
                log.warn("Unmapped Stripe status: {}", status);
                yield null;
            }
        };
    }
}
