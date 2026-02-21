package org.example.securitypractica.infrastucture;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.example.securitypractica.config.MinioProperties;
import org.example.securitypractica.exception.StorageException;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Repository
public class MinioStorageClient {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioStorageClient(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public boolean exists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new StorageException("MinIO stat error for path: " + path, e);
        } catch (Exception e) {
            throw new StorageException("Unexpected error checking existence: " + path, e);
        }
    }

    public Iterable<Result<Item>> list(String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .prefix(prefix)
                        .recursive(recursive)
                        .build()
        );
    }

    public void createFolder(String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
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
                            .bucket(minioProperties.getBucketName())
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
                            .bucket(minioProperties.getBucketName())
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
                            .bucket(minioProperties.getBucketName())
                            .object(destinationPath)
                            .source(CopySource.builder()
                                    .bucket(minioProperties.getBucketName())
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
                            .bucket(minioProperties.getBucketName())
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
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

    public InputStream getStream(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Failed to get stream from MinIO", e);
        }
    }

}