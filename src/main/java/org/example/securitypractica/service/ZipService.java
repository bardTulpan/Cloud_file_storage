package org.example.securitypractica.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.securitypractica.config.MinioProperties;
import org.example.securitypractica.infrastucture.MinioStorageClient;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZipService {

    private final MinioStorageClient minioStorageClient;

    public void archiveFolder(String sourcePath, OutputStream outputStream) {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            Iterable<Result<Item>> results = minioStorageClient.list(sourcePath, true);

            boolean hasEntries = false;
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) continue;

                hasEntries = true;
                String entryName = item.objectName().substring(sourcePath.length());
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);

                try (InputStream inputStream = minioStorageClient.getStream(item.objectName())) {
                    inputStream.transferTo(zos);
                }
                zos.closeEntry();
            }

            if (!hasEntries) {
                throw new RuntimeException("Folder is empty, nothing to archive");
            }
            zos.finish();
        } catch (Exception e) {
            throw new RuntimeException("Error while creating zip archive: " + e.getMessage(), e);
        }
    }
}