package com.cybernode.ai.distributed_codeforge.workspace_service.repository;


import com.cybernode.ai.distributed_codeforge.workspace_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
