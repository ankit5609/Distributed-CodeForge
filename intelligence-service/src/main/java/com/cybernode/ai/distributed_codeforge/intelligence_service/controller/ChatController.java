package com.cybernode.ai.distributed_codeforge.intelligence_service.controller;


import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatRequest;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.ChatResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.StreamResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.AiGenerationService;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.ChatService;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.UsageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Validated
public class ChatController {

    private final AiGenerationService aiGenerationService;
    private final ChatService chatService;
    private final UsageService usageService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<ServerSentEvent<StreamResponse>> streamChat(
            @RequestParam("message") @NotBlank String message,
            @RequestParam("projectId") @NotNull Long projectId,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        return aiGenerationService.streamResponse(message, projectId, image)
                .map(data-> ServerSentEvent.<StreamResponse>builder()
                .data(data)
                .build());

    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable @NotNull @Min(1) Long projectId) {

        return ResponseEntity.ok(chatService.getProjectChatHistory(projectId));
    }

    @GetMapping("/internal/v1/usage/today")
    public ResponseEntity<Integer> getTokensUsedToday(@RequestParam("userId") @NotNull @Min(1) Long userId) {
        return ResponseEntity.ok(usageService.getTokensUsedToday(userId));
    }
}
