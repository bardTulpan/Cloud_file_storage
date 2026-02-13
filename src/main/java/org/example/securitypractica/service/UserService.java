package org.example.securitypractica.service;

import lombok.RequiredArgsConstructor;
import org.example.securitypractica.entity.User;
import org.example.securitypractica.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }


}