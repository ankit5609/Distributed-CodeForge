package com.cybernode.ai.distributed_codeforge.account_service.mapper;


import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.SubscriptionResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Plan;
import com.cybernode.ai.distributed_codeforge.account_service.entity.Subscription;
import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanSubscriptionMapper {

    @org.mapstruct.Mapping(target = "message", ignore = true)
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);

}
