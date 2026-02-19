package org.example.securitypractica.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
@Component
public class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName = "test-bucket";
}
