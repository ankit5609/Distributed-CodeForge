package com.cybernode.ai.distributed_codeforge.intelligence_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentLogsResponse {
    private Long projectId;
    private String status; // "RUNNING" or "CRASHED" or "UNREACHABLE"
    private String logs;
}
