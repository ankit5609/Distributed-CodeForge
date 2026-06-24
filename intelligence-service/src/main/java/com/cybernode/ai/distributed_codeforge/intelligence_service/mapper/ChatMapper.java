package com.cybernode.ai.distributed_codeforge.intelligence_service.mapper;


import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatEventResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatEvent;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {
    @Mapping(source = "chatEventType", target = "type")
    ChatEventResponse fromChatEvent(ChatEvent chatEvent);
    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);
}
