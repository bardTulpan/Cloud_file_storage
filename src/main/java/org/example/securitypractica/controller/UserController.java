package org.example.securitypractica.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.securitypractica.dto.UserMeResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Operation(summary = "Получение информации о текущем пользователе")
    @GetMapping("/me")
    public UserMeResponseDto getCurrentUser(Principal principal) {
        return new UserMeResponseDto(principal.getName());
    }

}
