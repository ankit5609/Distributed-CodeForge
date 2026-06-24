package com.cybernode.ai.distributed_codeforge.intelligence_service.entity;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.ChatEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "chat_events")
public class ChatEvent {

    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    ChatMessage chatMessage;

    @Column(nullable = false)
    Integer sequenceOrder;

    @Column(columnDefinition = "text")
    String content;

    String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatEventType chatEventType;

    @Column(columnDefinition = "text")
    String metadata;
}
