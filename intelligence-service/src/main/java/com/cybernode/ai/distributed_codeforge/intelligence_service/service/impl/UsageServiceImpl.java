package com.cybernode.ai.distributed_codeforge.intelligence_service.service.impl;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.intelligence_service.client.AccountClient;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.UsageLog;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.UsageLogRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {

    private final UsageLogRepository usageLogRepository;
    private final AuthUtil authUtil;
    private final AccountClient accountClient;

    @Override
    public void recordTokenUsage(Long userId, int actualToken) {
        LocalDate today = LocalDate.now();
        UsageLog todayLog=usageLogRepository.findByUserIdAndDate(userId,today)
                .orElseGet(()->createNewDailyUsageLog(userId,today));

        todayLog.setTokensUsed(todayLog.getTokensUsed()+actualToken);
        usageLogRepository.save(todayLog);
    }

    @Override
    public void checkDailyTokensUsage() {
        Long userId= authUtil.getCurrentUserId();
        PlanDto plan=accountClient.getCurrentSubscribedPlanByUser();

        LocalDate today=LocalDate.now();
        UsageLog todayLog=usageLogRepository.findByUserIdAndDate(userId,today)
                .orElseGet(()->createNewDailyUsageLog(userId,today));

        if(plan.unlimitedAi()) return;
        int currentUsage=todayLog.getTokensUsed();
        int limit=plan.maxTokensPerDay();
        if(currentUsage>=limit){
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit reached, Upgrade now");
        }


    }

    private UsageLog createNewDailyUsageLog(Long userId, LocalDate date){
        UsageLog newLog=UsageLog.builder()
                .userId(userId)
                .date(date)
                .tokensUsed(0)
                .build();
        return usageLogRepository.save(newLog);
    }

    @Override
    public int getTokensUsedToday(Long userId) {
        LocalDate today = LocalDate.now();
        return usageLogRepository.findByUserIdAndDate(userId, today)
                .map(UsageLog::getTokensUsed)
                .orElse(0);
    }
}
