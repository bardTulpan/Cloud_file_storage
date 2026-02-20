package org.example.securitypractica.util;

import io.minio.messages.Item;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.exception.InvalidPathException;
import org.springframework.stereotype.Service;

public class PathUtils {

    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        securityCheck(path);
        String res = path.startsWith("/") ? path.substring(1) : path;
        return res.replaceAll("/{2,}", "/");
    }

    public static void securityCheck(String path) {
        if (path.contains("..")) throw new InvalidPathException("Security violation");
    }

    public static String getFileNameFromPath(String path) {
        String c = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return c.substring(c.lastIndexOf("/") + 1);
    }

    public static String getParentPath(String path) {
        String t = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int last = t.lastIndexOf("/");
        return (last == -1) ? "" : path.substring(0, last + 1);
    }

    public static String normalizeDirectoryPath(String path) {
        String n = normalizePath(path);
        return (n.isEmpty() || n.endsWith("/")) ? n : n + "/";
    }

    public static String getUserRootPath(Long userId) {
        return "user-" + userId + "-files/";
    }
}
