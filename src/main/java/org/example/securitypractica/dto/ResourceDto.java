package org.example.securitypractica.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ResourceDto(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}

