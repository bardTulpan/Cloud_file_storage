package org.example.securitypractica.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Repository
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:test-backet}")
    private String bucketName;

    public boolean exists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(path)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) return false;
            throw new RuntimeException("MinIO stat error", e);
        } catch (Exception e) {
            return false;
        }
    }

    public Iterable<Result<Item>> list(String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build()
        );
    }

    public void createFolder(String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder", e);
        }
    }

    public void putFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload error", e);
        }
    }

    public InputStream getObject(String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO download error", e);
        }
    }

    public void copy(String sourcePath, String destinationPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destinationPath)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourcePath)
                                    .build())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO copy error", e);
        }
    }

    public void delete(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO delete error", e);
        }
    }

    public StatObjectResponse getMetadata(String path) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

}