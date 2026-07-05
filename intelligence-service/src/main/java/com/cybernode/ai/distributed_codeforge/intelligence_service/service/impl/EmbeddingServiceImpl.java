package com.cybernode.ai.distributed_codeforge.intelligence_service.service.impl;

import com.cybernode.ai.distributed_codeforge.intelligence_service.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Implementation of EmbeddingService managing pgvector store persistence
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final VectorStore vectorStore;

    @Override
    public void reindexFile(Long projectId, String path, String content) {
        String docId = UUID.nameUUIDFromBytes((projectId + ":" + path).getBytes()).toString();

        if (content == null || content.trim().isEmpty()) {
            log.info("Deleting empty or removed file from vector store: {}/{}", projectId, path);
            try {
                vectorStore.delete(List.of(docId));
            } catch (Exception e) {
                log.error("Failed to delete document {} from vector store", docId, e);
            }
            return;
        }

        log.info("Indexing file content into pgvector: {}/{}", projectId, path);
        try {
            // Tag with projectId and path in metadata for search filtering
            Document document = new Document(
                    docId,
                    content,
                    Map.of(
                            "projectId", projectId,
                            "path", path
                    )
            );
            vectorStore.add(List.of(document));
            log.info("Indexed successfully: {}/{} with ID: {}", projectId, path, docId);
        } catch (Exception e) {
            log.error("Failed to index document to pgvector: {}/{}", projectId, path, e);
        }
    }
}
