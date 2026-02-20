package org.example.securitypractica.service;

import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.example.securitypractica.config.MinioProperties;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.exception.FileAlreadyExistsException;
import org.example.securitypractica.exception.BadRequestException;
import org.example.securitypractica.exception.NotFoundException;
import org.example.securitypractica.exception.StorageException;
import org.example.securitypractica.infrastucture.MinioStorageClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class StorageService {

    private final MinioStorageClient minioStorageClient;
    private final ZipService zipService;
    private final PathService pathService;
    private final MinioProperties minioProperties;
    @Qualifier("storageExecutor")
    private final Executor storageExecutor;

    public StorageService(
            MinioStorageClient minioStorageClient,
            ZipService zipService,
            PathService pathService,
            MinioProperties minioProperties,
            Executor storageExecutor,
            @Value("${app.storage.threads:10}") int countOfThreads
    ) {
        this.minioStorageClient = minioStorageClient;
        this.zipService = zipService;
        this.pathService = pathService;
        this.minioProperties = minioProperties;
        this.storageExecutor = storageExecutor;
        log.info("StorageService initialized with {} threads", countOfThreads);
    }

    public ResourceDto getResource(String path, Long userId) {
        String normalized = pathService.normalizePath(path);
        String root = pathService.getUserRootPath(userId);
        String fullPath = root + normalized;

        if (normalized.isEmpty()) {
            return new ResourceDto("", "root", null, ResourceType.DIRECTORY);
        }

        if (fullPath.endsWith("/")) {
            if (minioStorageClient.exists(fullPath)) {
                return pathService.mapToDto(normalized, null, ResourceType.DIRECTORY);
            }
        } else {
            var metadata = minioStorageClient.getMetadata(fullPath);
            if (metadata != null) {
                return pathService.mapToDto(normalized, metadata.size(), ResourceType.FILE);
            }

            if (minioStorageClient.exists(fullPath + "/")) {
                return pathService.mapToDto(normalized + "/", null, ResourceType.DIRECTORY);
            }
        }

        throw new NotFoundException("Resource not found: " + path);
    }

    public ResourceDto createDirectory(String path, Long userId) {
        String normalized = pathService.normalizeDirectoryPath(path);
        String fullPath = pathService.getUserRootPath(userId) + normalized;

        if (minioStorageClient.exists(fullPath)) {
            throw new FileAlreadyExistsException("Directory already exists");
        }
        validateParentExists(normalized, userId);
        minioStorageClient.createFolder(fullPath);

        return pathService.mapToDto(normalized, null, ResourceType.DIRECTORY);
    }



    public List<ResourceDto> uploadFiles(String path, List<MultipartFile> files, Long userId) {

        String normalizedPath = pathService.normalizeDirectoryPath(path);
        validateParentExists(normalizedPath, userId);
        String rootPath = pathService.getUserRootPath(userId);

        List<CompletableFuture<ResourceDto>> futures = files.stream()
                .filter(file -> file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String filename = file.getOriginalFilename();
                        pathService.securityCheck(filename);
                        String fullPath = rootPath + normalizedPath + filename;

                        if (minioStorageClient.exists(fullPath)) {
                            throw new FileAlreadyExistsException("File already exists: " + filename);
                        }

                        try (InputStream is = file.getInputStream()) {
                            minioStorageClient.putFile(fullPath, is, file.getSize(), file.getContentType());
                        }

                        return pathService.mapToDto(normalizedPath + filename, file.getSize(), ResourceType.FILE);
                    } catch (FileAlreadyExistsException e) {
                        throw e;
                    } catch (Exception e) {
                        log.error("Upload error", e);
                        throw new StorageException("Failed to upload file", e);
                    }
                }, storageExecutor))
                .toList();


        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<ResourceDto> listItems(String path, Long userId) {
        String normalized = pathService.normalizeDirectoryPath(path);
        String fullPath = pathService.getUserRootPath(userId) + normalized;

        if (!normalized.isEmpty() && !minioStorageClient.exists(fullPath)) {
            throw new NotFoundException("Directory not found");
        }

        List<ResourceDto> dtos = new ArrayList<>();
        var results = minioStorageClient.list(fullPath, false);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                if (item.objectName().equals(fullPath)) continue;
                dtos.add(pathService.mapToResourceDto(item, userId));
            } catch (Exception e) {
                log.error("Error while getting listItems for user {} ", userId, e);
                throw new RuntimeException("ListItems error");
            }
        }
        return dtos;
    }

    public void deleteResource(String path, Long userId) {
        log.warn("User {} is deleting resource: {}", userId, path);
        String normalized = pathService.normalizePath(path);
        String fullPath = pathService.getUserRootPath(userId) + normalized;

        if (!minioStorageClient.exists(fullPath)) throw new NotFoundException("Not found");

        if (fullPath.endsWith("/")) {
            var items = minioStorageClient.list(fullPath, true);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Result<Item> result : items) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        String objectName = result.get().objectName();
                        minioStorageClient.delete(objectName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
                    }
                }, storageExecutor));
            }

            try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                throw new StorageException("Partial delete failure", (Exception) e.getCause());
            }
        }

        minioStorageClient.delete(fullPath);
    }

    public void move(String from, String to, Long userId) {
        String root = pathService.getUserRootPath(userId);
        String fullFrom = root + pathService.normalizePath(from);
        String fullTo = root + pathService.normalizePath(to);

        if (!minioStorageClient.exists(fullFrom)) throw new NotFoundException("Source not found");
        if (minioStorageClient.exists(fullTo)) throw new FileAlreadyExistsException("Target exists");

        var items = minioStorageClient.list(fullFrom, true);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Result<Item> result : items) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    String oldKey = result.get().objectName();
                    String newKey = fullTo + oldKey.substring(fullFrom.length());
                    minioStorageClient.copy(oldKey, newKey);
                    minioStorageClient.delete(oldKey);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy: " + e.getMessage(), e);
                }
            }, storageExecutor));
        }
        try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new StorageException("Partial copy failure", (Exception) e.getCause());
        }
    }

    public List<ResourceDto> search(String query, Long userId) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Empty query");
        }

        String root = pathService.getUserRootPath(userId);
        String lowerQuery = query.toLowerCase();

        Iterable<Result<Item>> results = minioStorageClient.list(root, true);

        return StreamSupport.stream(results.spliterator(), true)
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        log.error("Error getting item during search for user {}", userId, e);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .filter(item -> {
                    String fileName = pathService.getFileNameFromPath(item.objectName());
                    return fileName.toLowerCase().contains(lowerQuery);
                })

                .map(item -> pathService.mapToResourceDto(item, userId))

                .collect(Collectors.toList());
    }

    public void downloadResource(String path, Long userId, OutputStream outputStream) {
        String fullPath = pathService.getUserRootPath(userId) + pathService.normalizePath(path);
        if (fullPath.endsWith("/")) {
            zipService.archiveFolder(minioProperties.getBucketName(), fullPath, outputStream);
        } else {
            try (InputStream is = minioStorageClient.getObject(fullPath)) {
                is.transferTo(outputStream);
            } catch (Exception e) {
                throw new RuntimeException("Download error", e);
            }
        }
    }

    public void checkResourceExists(String path, Long userId) {
        if (!minioStorageClient.exists(pathService.getUserRootPath(userId) + pathService.normalizePath(path))) {
            throw new NotFoundException("Resource not found");
        }
    }


    private void validateParentExists(String path, Long userId) {
        String parent = pathService.getParentPath(path);
        if (!parent.isEmpty() && !minioStorageClient.exists(pathService.getUserRootPath(userId) + parent)) {
            throw new NotFoundException("Parent not found");
        }
    }
}