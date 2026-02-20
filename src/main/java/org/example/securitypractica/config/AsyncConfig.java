package org.example.securitypractica.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

    private final ThreadProperties threadProperties;

    @Bean("storageExecutor")
    public Executor storageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadProperties.getThreads());
        executor.setMaxPoolSize(threadProperties.getThreads());
        executor.setThreadNamePrefix("storage-");
        executor.initialize();
        return executor;
    }



}
