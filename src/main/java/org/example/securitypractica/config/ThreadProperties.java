package org.example.securitypractica.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
@Component
public class ThreadProperties {
    private int threads;
}
