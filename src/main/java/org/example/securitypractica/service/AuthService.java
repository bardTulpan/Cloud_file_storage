package org.example.securitypractica.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.securitypractica.dto.RegistrationDto;
import org.example.securitypractica.dto.RegistrationResponseDto;
import org.example.securitypractica.entity.User;
import org.example.securitypractica.exception.UsernameExistsException;
import org.example.securitypractica.mapper.UserMapper;
import org.example.securitypractica.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public RegistrationResponseDto register(RegistrationDto registrationDto) {
        User user = new User(registrationDto.getUsername(), registrationDto.getPassword());
        boolean usernameAlreadyTaken = userRepository.findByUsername(registrationDto.getUsername()).isPresent();

        if (usernameAlreadyTaken) {
            throw new UsernameExistsException("User with username: " + user.getUsername() + " is already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        return userMapper.toRegistrationResponseDto(user);
    }
}
