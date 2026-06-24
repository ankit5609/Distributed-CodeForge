package com.cybernode.ai.distributed_codeforge.intelligence_service.service.impl;


import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatMessage;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatSession;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatSessionId;
import com.cybernode.ai.distributed_codeforge.intelligence_service.mapper.ChatMapper;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.ChatMessageRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.ChatSessionRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final AuthUtil authUtil;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMapper chatMapper;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {

        Long userId= authUtil.getCurrentUserId();

        ChatSession chatSession=chatSessionRepository.getReferenceById(
                new ChatSessionId(projectId,userId)
        );
        List<ChatMessage> chatMessageList=chatMessageRepository.findByChatSession(chatSession);

        return chatMapper.fromListOfChatMessage(chatMessageList);

    }
}
