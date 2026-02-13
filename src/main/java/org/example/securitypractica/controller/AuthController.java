package org.example.securitypractica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.securitypractica.dto.RegistrationDto;
import org.example.securitypractica.dto.RegistrationResponseDto;
import org.example.securitypractica.dto.UserMeResponseDto;
import org.example.securitypractica.entity.User;
import org.example.securitypractica.exception.InvalidCredentialsException;
import org.example.securitypractica.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация и вход в систему")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }


    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пользователь уже существует")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public RegistrationResponseDto registration(@RequestBody @Valid RegistrationDto registrationDto) {
        User user = new User(registrationDto.getUsername(), registrationDto.getPassword());
        User savedUser = authService.register(user);
        return new RegistrationResponseDto(savedUser.getUsername());
    }


    @Operation(summary = "Вход в систему (получение сессии)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/sign-in")
    public RegistrationResponseDto signIn(@RequestBody RegistrationDto registrationDto, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(registrationDto.getUsername(), registrationDto.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return new RegistrationResponseDto(registrationDto.getUsername());

        } catch (BadCredentialsException | InternalAuthenticationServiceException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Operation(summary = "Получение информации о текущем пользователе")
    @GetMapping("/me")
    public UserMeResponseDto getCurrentUser(Principal principal) {
        return new UserMeResponseDto(principal.getName());
    }
}

