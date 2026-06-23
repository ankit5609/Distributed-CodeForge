package com.cybernode.ai.distributed_codeforge.workspace_service.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "minio")
public class StorageConfig {

    private String url;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,secretKey)
                .build();

    }
}
