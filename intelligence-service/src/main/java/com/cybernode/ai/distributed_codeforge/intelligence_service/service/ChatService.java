package com.cybernode.ai.distributed_codeforge.intelligence_service.service;


import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatResponse;

import java.util.List;

public interface ChatService {

    List<ChatResponse> getProjectChatHistory(Long projectId);
}
