package org.example.securitypractica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegistrationResponseDto {
    @NotBlank
    @Size(min = 6)
    private String username;

    public RegistrationResponseDto(@NotBlank @Size(min = 6) String username) {
        this.username = username;
    }
}
