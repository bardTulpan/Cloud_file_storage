package org.example.securitypractica.mapper;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.util.PathUtils;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public static ResourceDto mapToDto(String relPath, Long size, ResourceType type) {
        return new ResourceDto(PathUtils.getParentPath(relPath), PathUtils.getFileNameFromPath(relPath), size, type);
    }

    public static ResourceDto mapToResourceDto(Item item, Long userId) {
        String full = item.objectName();
        String rel = full.substring(PathUtils.getUserRootPath(userId).length());
        boolean isDir = full.endsWith("/");
        String name = PathUtils.getFileNameFromPath(full);
        String parent = rel.substring(0, rel.length() - (isDir ? name.length() + 1 : name.length()));
        return new ResourceDto(parent, name, isDir ? null : item.size(), isDir ? ResourceType.DIRECTORY : ResourceType.FILE);
    }
}
