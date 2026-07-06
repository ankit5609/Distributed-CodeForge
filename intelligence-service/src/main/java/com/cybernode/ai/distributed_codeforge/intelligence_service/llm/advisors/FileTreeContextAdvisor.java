package com.cybernode.ai.distributed_codeforge.intelligence_service.llm.advisors;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileNode;
import com.cybernode.ai.distributed_codeforge.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final WorkspaceClient workspaceClient;
    private final VectorStore vectorStore;



    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
        Map<String,Object> context=request.context();
        Long projectId=Long.parseLong(context.getOrDefault("projectId",0).toString());
        ChatClientRequest augmentedChatClientRequest=augmentRequestWithFileTree(request,projectId);
        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest augmentRequestWithFileTree(ChatClientRequest request, Long projectId) {
        List<Message> incomingMessages = request.prompt().getInstructions();
        Message systemMessage = incomingMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .findFirst()
                .orElse(null);

        List<Message> userMessages = incomingMessages.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<Message> allMessages = new ArrayList<>();

        // Add original system message
        if (systemMessage != null) {
            allMessages.add(systemMessage);
        }

        // Add file tree (just paths, lightweight)
        List<FileNode> fileTree = workspaceClient.getFileTree(projectId).files();
        String fileTreeContext = "\n\n ---- FILE_TREE ----\n\n" + fileTree.toString();
        allMessages.add(new SystemMessage(fileTreeContext));

        // Retrieve relevant file contents dynamically via pgvector similarity search
        String userQuestion = userMessages.isEmpty() ? "" : userMessages.get(userMessages.size() - 1).getText();
        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            try {
                boolean bugReport = isBugReport(userQuestion);
                int topK = bugReport ? 10 : 5;

                List<Document> relevantDocuments = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(userQuestion)
                                .filterExpression("projectId == " + projectId)
                                .topK(topK)
                                .build()
                );

                List<String> fetchedPaths = new ArrayList<>();
                if (relevantDocuments != null) {
                    for (Document doc : relevantDocuments) {
                        Object pathObj = doc.getMetadata().get("path");
                        if (pathObj != null) {
                            fetchedPaths.add(pathObj.toString());
                        }
                    }
                }

                // If it is a bug report, check for explicit filename mentions to pull directly
                List<String> explicitFilesToFetch = new ArrayList<>();
                if (bugReport) {
                    String[] words = userQuestion.split("\\s+");
                    List<String> stopWords = List.of("the", "and", "for", "bug", "fix", "code", "file", "page", "this", "that", "with", "replicates", "component");
                    for (String word : words) {
                        String cleanWord = word.replaceAll("[^a-zA-Z0-9\\.\\-_/]", "");
                        if (cleanWord.isEmpty() || cleanWord.length() <= 2) continue;
                        String lowerWord = cleanWord.toLowerCase();
                        if (stopWords.contains(lowerWord)) continue;

                        for (FileNode node : fileTree) {
                            String path = node.path();
                            String lowerPath = path.toLowerCase();
                            String fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
                            String lowerFileName = fileName.toLowerCase();

                            String nameWithoutExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
                            String lowerNameWithoutExt = nameWithoutExt.toLowerCase();

                            if (lowerWord.equals(lowerFileName) || lowerWord.equals(lowerNameWithoutExt)) {
                                explicitFilesToFetch.add(path);
                            } else if (lowerFileName.contains(lowerWord) || lowerPath.contains("/" + lowerWord)) {
                                explicitFilesToFetch.add(path);
                            }
                        }
                    }
                }

                StringBuilder contextBuilder = new StringBuilder("\n\n ---- RELEVANT_CODE_CONTEXT ----\n");
                boolean hasContent = false;

                if (relevantDocuments != null && !relevantDocuments.isEmpty()) {
                    hasContent = true;
                    for (Document doc : relevantDocuments) {
                        String path = doc.getMetadata().getOrDefault("path", "unknown").toString();
                        contextBuilder.append("\n--- START OF FILE: ").append(path).append(" ---\n")
                                      .append(doc.getText())
                                      .append("\n--- END OF FILE ---\n");
                    }
                }

                for (String path : explicitFilesToFetch) {
                    if (!fetchedPaths.contains(path)) {
                        try {
                            String content = workspaceClient.getFileContent(projectId, path);
                            contextBuilder.append("\n--- START OF FILE: ").append(path).append(" ---\n")
                                          .append(content)
                                          .append("\n--- END OF FILE ---\n");
                            fetchedPaths.add(path);
                            hasContent = true;
                            log.info("Aggressively fetched explicit file from user visual bug report query: {}", path);
                        } catch (Exception e) {
                            log.warn("Failed to aggressively fetch file content for path: {}", path, e);
                        }
                    }
                }

                if (hasContent) {
                    allMessages.add(new SystemMessage(contextBuilder.toString()));
                }
            } catch (Exception e) {
                log.error("Failed to perform similarity search for project: {}", projectId, e);
            }
        }


        allMessages.addAll(userMessages);

        return request.mutate().prompt(new Prompt(allMessages, request.prompt().getOptions())).build();
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean isBugReport(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return q.contains("bug") || q.contains("error") || q.contains("broken") || q.contains("fail") ||
               q.contains("wrong") || q.contains("issue") || q.contains("fix") || q.contains("align") ||
               q.contains("misalign") || q.contains("spacing") || q.contains("color") || q.contains("look") ||
               q.contains("render") || q.contains("incorrect") || q.contains("problem") || q.contains("disappear") ||
               q.contains("hidden") || q.contains("crash") || q.contains("off");
    }
}
