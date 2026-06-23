package com.cybernode.ai.distributed_codeforge.account_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
//@ComponentScan(basePackages = {
//		"com.cybernode.ai.distributed_codeforge.account_service",
//		"com.cybernode.ai.distributed_codeforge.common_lib"})
public class AccountServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountServiceApplication.class, args);
	}

}
