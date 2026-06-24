package com.cybernode.ai.distributed_codeforge.intelligence_service.service;

public interface UsageService {
    void recordTokenUsage(Long userId,int actualToken);
    void checkDailyTokensUsage();
}
