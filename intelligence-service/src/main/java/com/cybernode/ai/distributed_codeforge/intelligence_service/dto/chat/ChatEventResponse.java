package com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.ChatEventType;

public record ChatEventResponse(
        Long id,
        ChatEventType type,
        Integer sequenceOrder,
        String content,
        String filePath,
        String metadata
) {
}
