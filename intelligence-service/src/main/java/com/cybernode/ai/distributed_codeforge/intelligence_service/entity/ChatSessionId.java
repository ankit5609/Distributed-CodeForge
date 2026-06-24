package com.cybernode.ai.distributed_codeforge.intelligence_service.entity;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class ChatSessionId implements Serializable {
    Long projectId;
    Long userId;
}
