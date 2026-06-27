package com.cybernode.ai.distributed_codeforge.account_service.service.impl;

import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.PortalResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Plan;
import com.cybernode.ai.distributed_codeforge.account_service.entity.StripeEvent;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.account_service.repository.PlanRepository;
import com.cybernode.ai.distributed_codeforge.account_service.repository.StripeEventRepository;
import com.cybernode.ai.distributed_codeforge.account_service.repository.UserRepository;
import com.cybernode.ai.distributed_codeforge.account_service.service.PaymentProcessor;
import com.cybernode.ai.distributed_codeforge.account_service.service.SubscriptionService;
import com.cybernode.ai.distributed_codeforge.common_lib.error.BadRequestException;
import com.cybernode.ai.distributed_codeforge.common_lib.error.ResourceNotFoundException;
import com.cybernode.ai.distributed_codeforge.common_lib.event.SubscriptionEvent;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentProcessor implements PaymentProcessor {

    private final AuthUtil authUtil;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final StripeEventRepository stripeEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request) {
        Plan plan = planRepository.findById(request.planId()).orElseThrow(() ->
                new ResourceNotFoundException("Plan", request.planId().toString()));

        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user", userId.toString()));

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
                .setSuccessUrl(frontendUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/cancel.html")
                .putMetadata("user_id", userId.toString())
                .putMetadata("plan_id", plan.getId().toString());

        try {
            String stripeCustomerId = user.getStripeCustomerId();
            if(stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                params.setCustomerEmail(user.getUsername());
            } else {
                params.setCustomer(stripeCustomerId); // stripe customer Id
            }
            Session session = Session.create(params.build()); // making api call to the Stripe Backend
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {
        Long userId = authUtil.getCurrentUserId();
        User user = getUser(userId);
        String stripeCustomerId = user.getStripeCustomerId();

        if(stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            throw new BadRequestException("User does not have a Stripe Customer Id, UserId:"+userId);
        }

        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontendUrl)
                            .build()
            );

            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // [IDEMPOTENCY]: Avoid double processing of webhooks
            if (stripeEventRepository.existsById(event.getId())) {
                log.warn("Duplicate Stripe webhook event received: {}. Ignoring.", event.getId());
                return;
            }

            // Persist Event ID immediately
            stripeEventRepository.save(StripeEvent.builder()
                    .id(event.getId())
                    .receivedAt(java.time.Instant.now())
                    .build());

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;

            if (deserializer.getObject().isPresent()) {
                stripeObject = deserializer.getObject().get();
            } else {
                try {
                    stripeObject = deserializer.deserializeUnsafe();
                    if (stripeObject == null) {
                        log.warn("Failed to deserialize webhook object for event: {}", event.getType());
                        return;
                    }
                } catch (Exception e) {
                    log.error("Unsafe deserialization failed for event {}: {}", event.getType(), e.getMessage());
                    throw new BadRequestException("Deserialization failed");
                }
            }

            Map<String, String> metadata = new HashMap<>();
            if (stripeObject instanceof Session session) {
                metadata = session.getMetadata();
                // Also update the user's stripeCustomerId if present in session
                String customerId = session.getCustomer();
                String userIdStr = metadata.get("user_id");
                if (userIdStr != null && customerId != null) {
                    Long userId = Long.parseLong(userIdStr);
                    User user = getUser(userId);
                    if (user.getStripeCustomerId() == null) {
                        user.setStripeCustomerId(customerId);
                        userRepository.save(user);
                        log.info("Saved stripe customer ID {} for user ID {}", customerId, userId);
                    }
                }
            }

            // [EVENT DRIVEN ARCHITECTURE]: Map Stripe webhook to internal Kafka SubscriptionEvent
            SubscriptionEvent subEvent = mapStripeToSubscriptionEvent(event.getType(), stripeObject, metadata);
            if (subEvent != null) {
                kafkaTemplate.send("subscription-events", subEvent.subscriptionId(), subEvent);
                log.info("Published subscription lifecycle event: {} to Kafka", subEvent);
            } else {
                log.debug("Skipped mapping/publishing for Stripe event type: {}", event.getType());
            }

        } catch (SignatureVerificationException e) {
            log.error("Signature verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid Stripe signature");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed", e);
            throw new RuntimeException("Webhook processing error", e);
        }
    }

    private SubscriptionEvent mapStripeToSubscriptionEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        switch (type) {
            case "checkout.session.completed" -> {
                if (stripeObject instanceof Session session) {
                    return new SubscriptionEvent(
                            "ACTIVATED",
                            Long.parseLong(metadata.get("user_id")),
                            Long.parseLong(metadata.get("plan_id")),
                            session.getSubscription(),
                            session.getCustomer(),
                            "incomplete",
                            null, null, null
                    );
                }
            }
            case "customer.subscription.updated" -> {
                if (stripeObject instanceof Subscription subscription) {
                    var item = subscription.getItems().getData().get(0);
                    return new SubscriptionEvent(
                            "UPDATED",
                            null, null,
                            subscription.getId(),
                            subscription.getCustomer(),
                            subscription.getStatus(),
                            Instant.ofEpochSecond(item.getCurrentPeriodStart()),
                            Instant.ofEpochSecond(item.getCurrentPeriodEnd()),
                            subscription.getCancelAtPeriodEnd()
                    );
                }
            }
            case "customer.subscription.deleted" -> {
                if (stripeObject instanceof Subscription subscription) {
                    return new SubscriptionEvent(
                            "CANCELLED",
                            null, null,
                            subscription.getId(),
                            subscription.getCustomer(),
                            "canceled",
                            null, null, null
                    );
                }
            }
            case "invoice.paid" -> {
                if (stripeObject instanceof Invoice invoice) {
                    String subId = extractSubscriptionId(invoice);
                    if (subId != null) {
                        return new SubscriptionEvent(
                                "INVOICE_PAID",
                                null, null,
                                subId,
                                invoice.getCustomer(),
                                null, null, null, null
                        );
                    }
                }
            }
            case "invoice.payment_failed" -> {
                if (stripeObject instanceof Invoice invoice) {
                    String subId = extractSubscriptionId(invoice);
                    if (subId != null) {
                        return new SubscriptionEvent(
                                "INVOICE_PAYMENT_FAILED",
                                null, null,
                                subId,
                                invoice.getCustomer(),
                                "past_due",
                                null, null, null
                        );
                    }
                }
            }
        }
        return null;
    }

    private String extractSubscriptionId(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) return null;

        var subDetails = parent.getSubscriptionDetails();
        if (subDetails == null) return null;

        return subDetails.getSubscription();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user", userId.toString()));
    }
}