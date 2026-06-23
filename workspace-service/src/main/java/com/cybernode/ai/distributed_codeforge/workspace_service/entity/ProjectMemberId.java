package com.cybernode.ai.distributed_codeforge.workspace_service.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class ProjectMemberId {
    Long userId;
    Long projectId;
}
