package com.cybernode.ai.distributed_codeforge.account_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class PaymentConfig {

    @Value("${stripe.api.secret}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Bean
    public NewTopic subscriptionEventsTopic() {
        return TopicBuilder.name("subscription-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
