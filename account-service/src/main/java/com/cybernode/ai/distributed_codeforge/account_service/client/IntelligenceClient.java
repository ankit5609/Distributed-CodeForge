package com.cybernode.ai.distributed_codeforge.account_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "intelligence-service", path = "/intelligence", url="${INTELLIGENCE_SERVICE_URI:}")
public interface IntelligenceClient {

    @GetMapping("/chat/internal/v1/usage/today")
    Integer getTokensUsedToday(@RequestParam("userId") Long userId);
}
