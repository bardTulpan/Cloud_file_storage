package org.example.securitypractica.service;

import jakarta.transaction.Transactional;
import org.example.securitypractica.dto.RegistrationDto;
import org.example.securitypractica.dto.RegistrationResponseDto;
import org.example.securitypractica.entity.User;
import org.example.securitypractica.exception.InvalidCredentialsException;
import org.example.securitypractica.mapper.UserMapper;
import org.example.securitypractica.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,  UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public RegistrationResponseDto register(RegistrationDto registrationDto) {
        User user = new User(registrationDto.getUsername(), registrationDto.getPassword());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new InvalidCredentialsException("User with username: " + user.getUsername() + " is already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        return userMapper.toRegistrationResponseDto(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
