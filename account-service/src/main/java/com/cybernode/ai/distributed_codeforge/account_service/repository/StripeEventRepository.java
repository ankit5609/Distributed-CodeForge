package com.cybernode.ai.distributed_codeforge.account_service.repository;

import com.cybernode.ai.distributed_codeforge.account_service.entity.StripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeEventRepository extends JpaRepository<StripeEvent, String> {
}
