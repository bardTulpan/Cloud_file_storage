package org.example.securitypractica.mapper;

import org.example.securitypractica.dto.RegistrationResponseDto;
import org.example.securitypractica.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public RegistrationResponseDto toRegistrationResponseDto(User user) {
        return new RegistrationResponseDto(user.getUsername());
    }
}
