package com.cybernode.ai.distributed_codeforge.account_service.repository;


import com.cybernode.ai.distributed_codeforge.account_service.entity.Subscription;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {

    /*
    * Get the current active Subscription
     */
    java.util.List<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionID);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);
}
