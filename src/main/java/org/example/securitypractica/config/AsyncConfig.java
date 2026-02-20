package org.example.securitypractica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${app.storage.threads:10}")
    private int storageThreads;

    @Bean("storageExecutor")
    public Executor storageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(storageThreads);
        executor.setMaxPoolSize(storageThreads);
        executor.setThreadNamePrefix("storage-");
        executor.initialize();
        return executor;
    }



}
