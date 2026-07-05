package com.cybernode.ai.distributed_codeforge.intelligence_service.service;

// Service to manage embedding operations and vector store indexing
public interface EmbeddingService {

    // Reindexes a file inside the vector database by its path and project ID
    void reindexFile(Long projectId, String path, String content);
}
