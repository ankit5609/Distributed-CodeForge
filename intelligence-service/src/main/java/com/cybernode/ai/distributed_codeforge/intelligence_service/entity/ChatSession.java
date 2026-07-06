package com.cybernode.ai.distributed_codeforge.intelligence_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "chat_sessions")
public class ChatSession {

    @EmbeddedId
    private ChatSessionId id;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    // Running summary of older messages pushed out of the rolling window
    @Column(columnDefinition = "text")
    String summary;

    // Latest message ID that has been integrated into the summary
    Long lastSummarizedMessageId;

    Instant deletedAt;

}
