package org.example.securitypractica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.service.StorageService;
import org.example.securitypractica.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
@Tag(name = "Directory Management", description = "Управление папками в Minio")
public class DirectoryController {

    private final StorageService storageService;
    private final UserService userService;

    @Operation(
            summary = "Создание новой папки",
            description = "Создает пустую папку. Родительская папка должна существовать."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Папка успешно создана"),
            @ApiResponse(responseCode = "400", description = "Невалидный путь"),
            @ApiResponse(responseCode = "404", description = "Родительская папка не найдена"),
            @ApiResponse(responseCode = "409", description = "Папка уже существует")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto createDirectory(
            @Parameter(description = "Путь к новой папке", example = "documents/")
            @RequestParam String path,
            Principal principal) {
        return storageService.createDirectory(path, getUserId(principal));
    }

    @Operation(summary = "Просмотр содержимого папки", description = "Возвращает список файлов и папок по указанному пути.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список успешно получен"),
            @ApiResponse(responseCode = "404", description = "Папка не найдена")
    })
    @GetMapping
    public List<ResourceDto> getDirectoryContent(
            @RequestParam(required = false, defaultValue = "") String path,
            Principal principal) {
        return storageService.listItems(path, getUserId(principal));
    }

    private Long getUserId(Principal principal) {
        return userService.findByUsername(principal.getName()).getId();
    }
}