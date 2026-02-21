package org.example.securitypractica.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceDto(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}

