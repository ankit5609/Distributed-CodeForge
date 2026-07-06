package com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.MessageRole;

import java.time.Instant;
import java.util.List;

public record ChatResponse(

        Long id,
        MessageRole role,
        List<ChatEventResponse> events,
        String content,
        Integer tokensUsed,
        Instant createdAt,
        String imageUrl

) {

}
