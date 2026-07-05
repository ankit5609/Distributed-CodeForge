package com.cybernode.ai.distributed_codeforge.intelligence_service.controller;

import com.cybernode.ai.distributed_codeforge.intelligence_service.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Controller to expose internal endpoint for file indexing
@RestController
@RequestMapping("/internal/v1/embeddings")
@RequiredArgsConstructor
@Slf4j
public class InternalEmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping("/reindex")
    public void reindexFile(
            @RequestParam("projectId") Long projectId,
            @RequestParam("path") String path,
            @RequestBody String content
    ) {
        log.info("Received internal reindex request for project {} file {}", projectId, path);
        embeddingService.reindexFile(projectId, path, content);
    }
}
