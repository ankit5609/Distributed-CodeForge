package com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotBlank(message = "Message is required") String message,
        @NotNull(message = "Project ID is required") Long projectId
) {
}
