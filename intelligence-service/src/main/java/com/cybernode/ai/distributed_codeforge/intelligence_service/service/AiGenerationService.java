package com.cybernode.ai.distributed_codeforge.intelligence_service.service;


import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.StreamResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long projectId, MultipartFile image);
}
