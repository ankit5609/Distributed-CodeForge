package com.cybernode.ai.distributed_codeforge.common_lib.event;

public record NotificationEvent(
        String type,
        Long userId,
        String message
) {}
