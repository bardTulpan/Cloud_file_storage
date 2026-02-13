package org.example.securitypractica.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZipService {

    private final MinioClient minioClient;

    public void archiveFolder(String bucketName, String sourcePath, OutputStream outputStream) {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(sourcePath)
                            .recursive(true)
                            .build()
            );

            boolean hasEntries = false;
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) continue;

                hasEntries = true;
                String entryName = item.objectName().substring(sourcePath.length());
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);

                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build())) {
                    is.transferTo(zos);
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