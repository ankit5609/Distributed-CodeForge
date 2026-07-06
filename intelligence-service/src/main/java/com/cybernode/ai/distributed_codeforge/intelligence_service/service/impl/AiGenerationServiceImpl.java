package com.cybernode.ai.distributed_codeforge.intelligence_service.service.impl;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.ChatEventStatus;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.ChatEventType;
import com.cybernode.ai.distributed_codeforge.common_lib.enums.MessageRole;
import com.cybernode.ai.distributed_codeforge.common_lib.event.FileStoreRequestEvent;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.intelligence_service.client.WorkspaceClient;
import com.cybernode.ai.distributed_codeforge.intelligence_service.dto.chat.StreamResponse;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatEvent;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatMessage;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatSession;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatSessionId;
import com.cybernode.ai.distributed_codeforge.intelligence_service.llm.LlmResponseParser;
import com.cybernode.ai.distributed_codeforge.intelligence_service.llm.PromptUtils;
import com.cybernode.ai.distributed_codeforge.intelligence_service.llm.advisors.FileTreeContextAdvisor;
import com.cybernode.ai.distributed_codeforge.intelligence_service.llm.tools.CodeGenerationTools;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.ChatEventRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.ChatMessageRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.repository.ChatSessionRepository;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.AiGenerationService;
import com.cybernode.ai.distributed_codeforge.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final LlmResponseParser llmResponseParser;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final WorkspaceClient workspaceClient;
    private final UsageService usageService;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final VectorStore vectorStore;



    private static final Pattern FILE_TAG_PATTERN =
            Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);
    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {
        usageService.checkDailyTokensUsage();
        Long userId= authUtil.getCurrentUserId();

        SecurityContext securityContext = SecurityContextHolder.getContext();
        ChatSession chatSession=createChatSessionIfNotExists(projectId,userId);

        // Load the last 10 messages for THIS project+user to avoid bloating the context window
        List<ChatMessage> recentList = chatMessageRepository.findTop10ByChatSessionOrderByIdDesc(chatSession);
        List<ChatMessage> chronological = new ArrayList<>(recentList);
        Collections.reverse(chronological);

        List<Message> history = chronological.stream()
                .map(m -> m.getRole() == MessageRole.USER
                        ? (Message) new UserMessage(m.getContent())
                        : (Message) new AssistantMessage(m.getContent() == null ? "" : m.getContent()))
                .toList();

        // Inject existing conversational summary if it exists into the system prompt context
        String systemPrompt = PromptUtils.CODE_GENERATION_SYSTEM_PROMPT;
        if (chatSession.getSummary() != null && !chatSession.getSummary().trim().isEmpty()) {
            systemPrompt += "\n\nHere is a summary of the older conversation history:\n" + chatSession.getSummary();
        }

        Map<String,Object> advisorParams=Map.of(
                "userId",userId,
                "projectId",projectId
        );

        StringBuilder fullResponseBuffer=new StringBuilder();
        CodeGenerationTools codeGenerationTools=new CodeGenerationTools(projectId,workspaceClient,vectorStore);

        AtomicReference<Long>  startTime=new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long>  endTime=new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(systemPrompt)
                .messages(history)

                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                    advisorSpec.params(advisorParams);
                    advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .doOnNext(response ->{

                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        String content = response.getResult().getOutput().getText();

                        if(content != null && !content.isEmpty() && endTime.get() == 0) { // first non-empty chunk received
                            endTime.set(System.currentTimeMillis());
                        }
                        if(response.getMetadata().getUsage() != null) {
                            usageRef.set(response.getMetadata().getUsage());
                        }
                        fullResponseBuffer.append(content);
                    }

                })
                .doOnComplete(()-> {
                    Schedulers.boundedElastic().schedule(()-> {
                        SecurityContextHolder.setContext(securityContext); // restore
                        try {
//                            parseAndSaveFiles(fullResponseBuffer.toString(), projectId);
                            Long duration=(endTime.get()-startTime.get())/1000;
                            finalizeChats(userMessage,chatSession, fullResponseBuffer.toString(),projectId,duration,usageRef.get(),userId);
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    });

                })
                .doOnError(error -> log.error("Error during streaming for project id: {}",projectId))
                .map(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        String text = response.getResult().getOutput().getText();
                        return new StreamResponse(text != null ? text : "");
                    }
                    return new StreamResponse("");
                });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession,String fullText, Long projectId, Long duration, Usage usage, Long userId){
        if(usage != null) {
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
        }
        //Save the User message
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(usage!=null?usage.getPromptTokens():0)
                .build()
        );

        //
        ChatMessage assistantChatMessage=ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.ASSISTANT)
                .content(fullText)
                .tokensUsed(usage.getCompletionTokens())
                .build();
        assistantChatMessage=chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList= llmResponseParser.parseChatEvents(fullText,assistantChatMessage);

        chatEventList.add(0,ChatEvent.builder()
                        .chatEventType(ChatEventType.THOUGHT)
                        .status(ChatEventStatus.CONFIRMED)
                        .chatMessage(assistantChatMessage)
                        .content("Thought for "+duration+"s")
                        .sequenceOrder(0)
                .build());

        chatEventList.stream()
                .filter(e-> e.getChatEventType()== ChatEventType.FILE_EDIT)
                .forEach(e-> {
                    String sagaId= UUID.randomUUID().toString();
                    e.setSagaId(sagaId);
                    FileStoreRequestEvent fileStoreRequestEvent=new FileStoreRequestEvent(
                            projectId,
                            sagaId,
                            e.getFilePath(),
                            e.getContent(),
                            userId
                    );
                    log.info("Storage request event sent: {}", e.getFilePath());
                    kafkaTemplate.send("file-storage-request-event","project-"+projectId,fileStoreRequestEvent);
                });

        chatEventRepository.saveAll(chatEventList);

        // Perform incremental summarization if the chat session grows beyond the 10-message rolling window
        try {
            ChatSession managedSession = chatSessionRepository.findById(chatSession.getId()).orElse(chatSession);
            List<ChatMessage> recentList = chatMessageRepository.findTop10ByChatSessionOrderByIdDesc(managedSession);
            
            if (recentList.size() == 10) {
                Long boundaryId = recentList.get(9).getId();
                List<ChatMessage> toSummarize = chatMessageRepository.findOlderMessagesToSummarize(
                        managedSession, 
                        boundaryId, 
                        managedSession.getLastSummarizedMessageId()
                );

                if (toSummarize != null && !toSummarize.isEmpty()) {
                    log.info("Summarizing {} older messages pushed out of the rolling window for session {}", toSummarize.size(), managedSession.getId());
                    StringBuilder conversationContext = new StringBuilder();
                    for (ChatMessage msg : toSummarize) {
                        conversationContext.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
                    }

                    String currentSummary = managedSession.getSummary() != null ? managedSession.getSummary() : "None";
                    String summarizationPrompt = "You are a conversational summary assistant. " +
                            "Your task is to update the existing summary of a developer chat session with the newly provided turns. " +
                            "Keep the summary concise, organized, and focused on the technical goals and files modified. Do not lose key context.\n\n" +
                            "Existing Summary:\n" + currentSummary + "\n\n" +
                            "New Turns to Add:\n" + conversationContext.toString() + "\n\n" +
                            "Provide only the updated summary text. Do not include any intro or outro comments.";

                    String newSummary = chatClient.prompt()
                            .user(summarizationPrompt)
                            .call()
                            .content();

                    if (newSummary != null && !newSummary.trim().isEmpty()) {
                        managedSession.setSummary(newSummary.trim());
                        managedSession.setLastSummarizedMessageId(toSummarize.get(toSummarize.size() - 1).getId());
                        chatSessionRepository.save(managedSession);
                        log.info("Successfully updated conversational summary for project: {}", projectId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to run incremental conversational summarization for project: {}", projectId, e);
        }
    }


//    private void parseAndSaveFiles(String fullResponse, Long projectId) {
//        Matcher matcher=FILE_TAG_PATTERN.matcher(fullResponse);
//
//        while(matcher.find()){
//            String filePath= matcher.group(1);
//            String fileContent= matcher.group(2).trim();
//            projectFileService.saveFile(filePath,fileContent,projectId);
//        }
//    }
    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId=new ChatSessionId(projectId,userId);
        ChatSession chatSession=chatSessionRepository.findById(chatSessionId).orElse(null);

        if(chatSession==null){
            chatSession=ChatSession.builder()
                    .id(chatSessionId)
                    .build();
            chatSession=chatSessionRepository.save(chatSession);

        }
        return chatSession;
    }
}
