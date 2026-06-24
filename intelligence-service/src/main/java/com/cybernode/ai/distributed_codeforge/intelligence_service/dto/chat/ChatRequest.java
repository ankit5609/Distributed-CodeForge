package com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat;

public record ChatRequest(
        String message,
        Long projectId
) {
}
