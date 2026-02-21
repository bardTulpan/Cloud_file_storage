package org.example.securitypractica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.exception.BadRequestException;
import org.example.securitypractica.service.StorageService;
import org.example.securitypractica.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Tag(name = "Resource Management", description = "Операции с файлами: загрузка, удаление, перемещение и поиск")
public class ResourceController {

    private final StorageService storageService;
    private final UserService userService;

    @Operation(
            summary = "Получить информацию о ресурсе",
            description = "Возвращает метаданные файла или папки (имя, путь, размер) по заданному пути."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация успешно получена"),
            @ApiResponse(responseCode = "404", description = "Ресурс по указанному пути не найден")
    })
    @GetMapping
    public ResourceDto getResource(
            @Parameter(description = "Путь к ресурсу", example = "documents/work/report.pdf")
            @RequestParam String path,
            Principal principal) {
        return storageService.getResource(path, getUserId(principal));
    }

    @Operation(summary = "Перемещение или переименование", description = "Переносит ресурс (файл или папку) по новому пути.")
    @PutMapping("/move")
    public ResourceDto move(
            @RequestParam String from,
            @RequestParam String to,
            Principal principal) {
        storageService.move(from, to, getUserId(principal));
        return storageService.getResource(to, getUserId(principal));

    }

    @Operation(summary = "Поиск", description = "Глобальный поиск файлов и папок по части имени.")
    @GetMapping("/search")
    public List<ResourceDto> search(@RequestParam String query, Principal principal) {
        return storageService.search(query, getUserId(principal));
    }

    @Operation(summary = "Загрузка файлов", description = "Позволяет загрузить один или несколько файлов в указанную папку.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceDto> uploadFiles(
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestPart("files") List<MultipartFile> files,
            Principal principal) {

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files selected for upload");
        }
        return storageService.uploadFiles(path, files, getUserId(principal));
    }

    @Operation(summary = "Удаление", description = "Удаляет файл или папку рекурсивно.")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path, Principal principal) {
        storageService.deleteResource(path, getUserId(principal));
    }

    @Operation(summary = "Скачивание", description = "Скачивает файл или папку (в виде ZIP-архива).")
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = userService.findByUsername(userDetails.getUsername()).getId();

        storageService.checkResourceExists(path, userId);

        String fileName = determineFileName(path);
        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(outputStream -> storageService.downloadResource(path, userId, outputStream));
    }

    private String determineFileName(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) return "root.zip";
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        String name = cleanPath.substring(cleanPath.lastIndexOf("/") + 1);
        return path.endsWith("/") ? name + ".zip" : name;
    }

    private Long getUserId(Principal principal) {
        return userService.findByUsername(principal.getName()).getId();
    }
}