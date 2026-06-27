package com.cybernode.ai.distributed_codeforge.workspace_service.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config = new ConfigBuilder()
                .withConnectionTimeout(60000)  // 1 minute (default 10s)
                .withRequestTimeout(300000)    // 5 minutes (default 30s)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
