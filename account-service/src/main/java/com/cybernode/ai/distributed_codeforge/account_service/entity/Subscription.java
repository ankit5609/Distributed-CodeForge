package com.cybernode.ai.distributed_codeforge.account_service.entity;

import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name="user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name="plan_id")
    Plan plan;

    String stripeSubscriptionId;     // should be called gatewaySubscriptionId

    @Enumerated(value = EnumType.STRING)
    SubscriptionStatus status;

    Instant currentPeriodStart;

    Instant currentPeriodEnd;

    Boolean cancelAtPeriodEnd;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;
}
