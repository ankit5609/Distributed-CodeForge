package com.cybernode.ai.distributed_codeforge.intelligence_service.service.impl;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.PlanDto;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.intelligence_service.client.AccountClient;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.UsageLog;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.UsageLogRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.kafka.core.KafkaTemplate;
import com.cybernode.ai.distributed_codeforge.common_lib.event.NotificationEvent;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageServiceImpl implements UsageService {

    private final UsageLogRepository usageLogRepository;
    private final AuthUtil authUtil;
    private final AccountClient accountClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
            sendNotificationEvent("TOKEN_LIMIT_REACHED", userId, "Daily AI token usage limit reached (" + limit + " tokens)");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit reached, Upgrade now");
        }


    }

    private void sendNotificationEvent(String type, Long userId, String message) {
        try {
            NotificationEvent event = new NotificationEvent(type, userId, message);
            kafkaTemplate.send("notification-events", userId != null ? userId.toString() : "global", event);
            log.info("Successfully published Kafka notification event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish Kafka notification event for user: {}", userId, e);
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
