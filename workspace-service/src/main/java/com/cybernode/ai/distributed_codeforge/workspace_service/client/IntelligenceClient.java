package com.cybernode.ai.distributed_codeforge.workspace_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

// OpenFeign client to communicate with intelligence-service for RAG indexing
@FeignClient(name = "intelligence-service", path = "/intelligence", url = "${INTELLIGENCE_SERVICE_URI:}")
public interface IntelligenceClient {

    @PostMapping("/internal/v1/embeddings/reindex")
    void reindexFile(
            @RequestParam("projectId") Long projectId,
            @RequestParam("path") String path,
            @RequestBody String content
    );
}
