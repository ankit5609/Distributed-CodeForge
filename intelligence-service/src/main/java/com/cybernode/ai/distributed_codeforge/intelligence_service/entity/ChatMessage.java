package com.cybernode.ai.distributed_codeforge.intelligence_service.entity;

import com.cybernode.ai.distributed_codeforge.common_lib.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumns(
            {
                    @JoinColumn(name="project_id",referencedColumnName = "projectId",nullable = false),
                    @JoinColumn(name="user_id",referencedColumnName = "userId",nullable = false)
            }
    )
    ChatSession chatSession;

    @Enumerated(EnumType.STRING)
            @Column(nullable = false)
    MessageRole role;

    @OneToMany(mappedBy = "chatMessage",cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @OrderBy("sequenceOrder ASC")
    List<ChatEvent> events; // empty um;ess ASSISTANT message

    @Column(columnDefinition ="text")
    String content; // NULL unless USER role

    Integer tokensUsed;

    @CreationTimestamp
    Instant createdAt;

}
