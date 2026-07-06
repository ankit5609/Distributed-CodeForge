package com.cybernode.ai.distributed_codeforge.intelligence_service.repository;

import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatMessage;
import com.cybernode.ai.distributed_codeforge.intelligence_service.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {

    @Query("""
            SELECT DISTINCT m FROM ChatMessage m
            LEFT JOIN FETCH m.events e
            WHERE m.chatSession = :chatSession
            ORDER BY m.createdAt ASC, e.sequenceOrder ASC
            """)
    List<ChatMessage> findByChatSession(ChatSession chatSession);

    // Fetch the last 10 messages of a chat session in descending order
    List<ChatMessage> findTop10ByChatSessionOrderByIdDesc(ChatSession chatSession);

    // Fetch older messages in a session up to the boundary ID (exclusive) that are greater than a min ID (exclusive)
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatSession = :chatSession
              AND m.id < :boundaryId
              AND (:minId IS NULL OR m.id > :minId)
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findOlderMessagesToSummarize(
            ChatSession chatSession, 
            Long boundaryId, 
            Long minId
    );


}
