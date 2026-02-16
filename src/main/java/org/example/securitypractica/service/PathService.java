package org.example.securitypractica.service;

import io.minio.messages.Item;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.exception.InvalidPathException;
import org.springframework.stereotype.Service;

@Service
public class PathService {

    public String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        securityCheck(path);
        String res = path.startsWith("/") ? path.substring(1) : path;
        return res.replaceAll("/{2,}", "/");
    }

    public void securityCheck(String path) {
        if (path.contains("..")) throw new InvalidPathException("Security violation");
    }

    public ResourceDto mapToResourceDto(Item item, Long userId) {
        String full = item.objectName();
        String rel = full.substring(getUserRootPath(userId).length());
        boolean isDir = full.endsWith("/");
        String name = getFileNameFromPath(full);
        String parent = rel.substring(0, rel.length() - (isDir ? name.length() + 1 : name.length()));
        return new ResourceDto(parent, name, isDir ? null : item.size(), isDir ? ResourceType.DIRECTORY : ResourceType.FILE);
    }

    public String getFileNameFromPath(String path) {
        String c = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return c.substring(c.lastIndexOf("/") + 1);
    }

    public String getParentPath(String path) {
        String t = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int last = t.lastIndexOf("/");
        return (last == -1) ? "" : path.substring(0, last + 1);
    }

    public ResourceDto mapToDto(String relPath, Long size, ResourceType type) {
        return new ResourceDto(getParentPath(relPath), getFileNameFromPath(relPath), size, type);
    }

    public String normalizeDirectoryPath(String path) {
        String n = normalizePath(path);
        return (n.isEmpty() || n.endsWith("/")) ? n : n + "/";
    }

    public String getUserRootPath(Long userId) {
        return "user-" + userId + "-files/";
    }
}
