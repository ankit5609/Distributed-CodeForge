package com.cybernode.ai.distributed_codeforge.workspace_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling activates Spring's @Scheduled annotation processing.
// Without this, any @Scheduled methods in the application will be silently ignored.
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class WorkspaceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkspaceServiceApplication.class, args);
	}

}
