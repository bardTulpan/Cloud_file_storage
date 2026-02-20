package org.example.securitypractica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.securitypractica.dto.RegistrationDto;
import org.example.securitypractica.dto.RegistrationResponseDto;
import org.example.securitypractica.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация и вход в систему")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,  SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }


    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пользователь уже существует")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public RegistrationResponseDto registration(@RequestBody @Valid RegistrationDto registrationDto, HttpServletRequest request, HttpServletResponse response) {
        RegistrationResponseDto registrationResponseDto =  authService.register(registrationDto);
        authenticateAndSaveContext(registrationDto, request, response);

        return registrationResponseDto;
    }

    @Operation(summary = "Вход в систему (получение сессии)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/sign-in")
    public RegistrationResponseDto signIn(@RequestBody @Valid RegistrationDto registrationDto, HttpServletRequest request, HttpServletResponse response) {
        authenticateAndSaveContext(registrationDto, request, response);

        return new RegistrationResponseDto(registrationDto.getUsername());
    }

    private void authenticateAndSaveContext(RegistrationDto registrationDto, HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(registrationDto.getUsername(), registrationDto.getUsername());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);
    }
}

