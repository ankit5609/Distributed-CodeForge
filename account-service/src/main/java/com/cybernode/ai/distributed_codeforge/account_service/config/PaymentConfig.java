package com.cybernode.ai.distributed_codeforge.account_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Value("${stripe.api.secret}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @org.springframework.context.annotation.Bean
    public org.apache.kafka.clients.admin.NewTopic subscriptionEventsTopic() {
        return org.springframework.kafka.config.TopicBuilder.name("subscription-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
