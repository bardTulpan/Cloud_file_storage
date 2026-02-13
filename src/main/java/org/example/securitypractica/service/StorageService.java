package org.example.securitypractica.service;

import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.exception.FileAlreadyExistsException;
import org.example.securitypractica.exception.InvalidPathException;
import org.example.securitypractica.exception.MyBadRequestException;
import org.example.securitypractica.exception.NotFoundException;
import org.example.securitypractica.repository.MinioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioRepository minioRepository;
    private final ZipService zipService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private String getUserRootPath(Long userId) {
        return "user-" + userId + "-files/";
    }

    public ResourceDto getResource(String path, Long userId) {
        String normalized = normalizePath(path);
        String root = getUserRootPath(userId);
        String fullPath = root + normalized;

        if (normalized.isEmpty()) {
            return new ResourceDto("", "root", null, ResourceType.DIRECTORY);
        }

        if (fullPath.endsWith("/")) {
            if (minioRepository.exists(fullPath)) {
                return mapToDto(normalized, null, ResourceType.DIRECTORY);
            }
        } else {
            var metadata = minioRepository.getMetadata(fullPath);
            if (metadata != null) {
                return mapToDto(normalized, metadata.size(), ResourceType.FILE);
            }

            if (minioRepository.exists(fullPath + "/")) {
                return mapToDto(normalized + "/", null, ResourceType.DIRECTORY);
            }
        }

        throw new NotFoundException("Resource not found: " + path);
    }

    public ResourceDto createDirectory(String path, Long userId) {
        String normalized = normalizeDirectoryPath(path);
        String fullPath = getUserRootPath(userId) + normalized;

        if (minioRepository.exists(fullPath)) {
            throw new FileAlreadyExistsException("Directory already exists");
        }
        validateParentExists(normalized, userId);
        minioRepository.createFolder(fullPath);

        return mapToDto(normalized, null, ResourceType.DIRECTORY);
    }

    public List<ResourceDto> uploadFiles(String path, List<MultipartFile> files, Long userId) {
        String normalizedPath = normalizeDirectoryPath(path);
        validateParentExists(normalizedPath, userId);
        String rootPath = getUserRootPath(userId);
        List<ResourceDto> results = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) continue;

            securityCheck(originalFilename);
            String fullPath = rootPath + normalizedPath + originalFilename;

            if (minioRepository.exists(fullPath)) {
                throw new FileAlreadyExistsException("File already exists: " + originalFilename);
            }

            try {
                minioRepository.putFile(fullPath, file.getInputStream(), file.getSize(), file.getContentType());
                results.add(mapToDto(normalizedPath + originalFilename, file.getSize(), ResourceType.FILE));
            } catch (Exception e) {
                throw new RuntimeException("Upload failed", e);
            }
        }
        return results;
    }

    public List<ResourceDto> listItems(String path, Long userId) {
        String normalized = normalizeDirectoryPath(path);
        String fullPath = getUserRootPath(userId) + normalized;

        if (!normalized.isEmpty() && !minioRepository.exists(fullPath)) {
            throw new NotFoundException("Directory not found");
        }

        List<ResourceDto> dtos = new ArrayList<>();
        var results = minioRepository.list(fullPath, false);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                if (item.objectName().equals(fullPath)) continue;
                dtos.add(mapToResourceDto(item, userId));
            } catch (Exception e) {
                log.error("Error while getting listItems for user {} ", userId, e);
                throw new RuntimeException("ListItems error");
            }
        }
        return dtos;
    }

    public void deleteResource(String path, Long userId) {
        String normalized = normalizePath(path);
        String fullPath = getUserRootPath(userId) + normalized;

        if (!minioRepository.exists(fullPath)) throw new NotFoundException("Not found");

        if (fullPath.endsWith("/")) {
            var items = minioRepository.list(fullPath, true);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Result<Item> result : items) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        String objectName = result.get().objectName();
                        minioRepository.delete(objectName);
                    } catch (Exception e) {
                        log.error("Failed to delete object: " + e.getMessage());
                    }
                }));
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }
        minioRepository.delete(fullPath);
    }

    public void move(String from, String to, Long userId) {
        String root = getUserRootPath(userId);
        String fullFrom = root + normalizePath(from);
        String fullTo = root + normalizePath(to);

        if (!minioRepository.exists(fullFrom)) throw new NotFoundException("Source not found");
        if (minioRepository.exists(fullTo)) throw new FileAlreadyExistsException("Target exists");

        var items = minioRepository.list(fullFrom, true);
        for (Result<Item> result : items) {
            try {
                String oldKey = result.get().objectName();
                String newKey = fullTo + oldKey.substring(fullFrom.length());
                minioRepository.copy(oldKey, newKey);
                minioRepository.delete(oldKey);
            } catch (Exception e) {
                throw new RuntimeException("Move error", e);
            }
        }
    }

    public List<ResourceDto> search(String query, Long userId) {
        if (query == null || query.isBlank()) throw new MyBadRequestException("Empty query");
        String root = getUserRootPath(userId);
        List<ResourceDto> found = new ArrayList<>();
        var items = minioRepository.list(root, true);
        for (Result<Item> result : items) {
            try {
                Item item = result.get();
                if (getFileNameFromPath(item.objectName()).toLowerCase().contains(query.toLowerCase())) {
                    found.add(mapToResourceDto(item, userId));
                }
            } catch (Exception e) {
                log.error("Error while getting object from minio for user {}", userId, e);
                throw new RuntimeException("Search errorig");
            }
        }
        return found;
    }

    public void downloadResource(String path, Long userId, OutputStream outputStream) {
        String fullPath = getUserRootPath(userId) + normalizePath(path);
        if (fullPath.endsWith("/")) {
            zipService.archiveFolder(bucketName, fullPath, outputStream);
        } else {
            try (InputStream is = minioRepository.getObject(fullPath)) {
                is.transferTo(outputStream);
            } catch (Exception e) {
                throw new RuntimeException("Download error", e);
            }
        }
    }

    public void checkResourceExists(String path, Long userId) {
        if (!minioRepository.exists(getUserRootPath(userId) + normalizePath(path))) {
            throw new NotFoundException("Resource not found");
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        securityCheck(path);
        String res = path.startsWith("/") ? path.substring(1) : path;
        return res.replaceAll("/{2,}", "/");
    }

    private String normalizeDirectoryPath(String path) {
        String n = normalizePath(path);
        return (n.isEmpty() || n.endsWith("/")) ? n : n + "/";
    }

    private void securityCheck(String path) {
        if (path.contains("..")) throw new InvalidPathException("Security violation");
    }

    private void validateParentExists(String path, Long userId) {
        String parent = getParentPath(path);
        if (!parent.isEmpty() && !minioRepository.exists(getUserRootPath(userId) + parent)) {
            throw new NotFoundException("Parent not found");
        }
    }

    private String getParentPath(String path) {
        String t = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int last = t.lastIndexOf("/");
        return (last == -1) ? "" : path.substring(0, last + 1);
    }

    private String getFileNameFromPath(String path) {
        String c = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return c.substring(c.lastIndexOf("/") + 1);
    }

    private ResourceDto mapToResourceDto(Item item, Long userId) {
        String full = item.objectName();
        String rel = full.substring(getUserRootPath(userId).length());
        boolean isDir = full.endsWith("/");
        String name = getFileNameFromPath(full);
        String parent = rel.substring(0, rel.length() - (isDir ? name.length() + 1 : name.length()));
        return new ResourceDto(parent, name, isDir ? null : item.size(), isDir ? ResourceType.DIRECTORY : ResourceType.FILE);
    }

    private ResourceDto mapToDto(String relPath, Long size, ResourceType type) {
        return new ResourceDto(getParentPath(relPath), getFileNameFromPath(relPath), size, type);
    }
}