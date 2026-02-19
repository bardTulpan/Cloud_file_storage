package org.example.securitypractica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SecurityPracticaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityPracticaApplication.class, args);
    }

}
