package com.cybernode.ai.distributed_codeforge.workspace_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "projects",
    indexes = {
        @Index(name="idx_projects_updated_at_desc",columnList="updated_at DESC, deleted_at"),
            @Index(name = "idx_project_deleted_at", columnList = "deleted_at")
    })
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    private Boolean isPublic;

    private Instant createdAt;

    private Instant updatedAt;
    @Setter
    private Instant deletedAt;

    @PrePersist
    void setCreatedAt(){
        this.createdAt=Instant.now();
        this.updatedAt=Instant.now();
    }
    @PreUpdate
    void setUpdatedAt(){
        this.updatedAt=Instant.now();
    }
}
