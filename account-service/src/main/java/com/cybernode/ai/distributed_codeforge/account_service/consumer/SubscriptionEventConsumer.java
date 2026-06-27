package com.cybernode.ai.distributed_codeforge.account_service.consumer;

import com.cybernode.ai.distributed_codeforge.account_service.service.SubscriptionService;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;
import com.cybernode.ai.distributed_codeforge.common_lib.event.SubscriptionEvent;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventConsumer {

    private final SubscriptionService subscriptionService;

    @KafkaListener(topics = "subscription-events", groupId = "codeforge-group")
    public void consumeSubscriptionEvent(SubscriptionEvent event) {
        log.info("Received subscription event from Kafka: {}", event);

        try {
            switch (event.eventType()) {
                case "ACTIVATED" -> {
                    subscriptionService.activateSubscription(
                            event.userId(),
                            event.planId(),
                            event.subscriptionId(),
                            event.customerId()
                    );
                }
                case "UPDATED" -> {
                    SubscriptionStatus status = mapStripeStatusToEnum(event.status());
                    subscriptionService.updateSubscription(
                            event.subscriptionId(),
                            status,
                            event.periodStart(),
                            event.periodEnd(),
                            event.cancelAtPeriodEnd(),
                            event.planId()
                    );
                }
                case "CANCELLED" -> {
                    subscriptionService.cancelSubscription(event.subscriptionId());
                }
                case "INVOICE_PAID" -> {
                    // Fetch period from Stripe
                    Subscription subscription = Subscription.retrieve(event.subscriptionId());
                    var item = subscription.getItems().getData().get(0);
                    Instant periodStart = Instant.ofEpochSecond(item.getCurrentPeriodStart());
                    Instant periodEnd = Instant.ofEpochSecond(item.getCurrentPeriodEnd());

                    subscriptionService.renewSubscriptionPeriod(
                            event.subscriptionId(),
                            periodStart,
                            periodEnd
                    );
                }
                case "INVOICE_PAYMENT_FAILED" -> {
                    subscriptionService.markSubscriptionPastDue(event.subscriptionId());
                }
                default -> log.warn("Unknown internal subscription event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process subscription event: {}", event, e);
            throw new RuntimeException("Kafka message processing failed", e);
        }
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid", "paused", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "cancelled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> null;
        };
    }
}
