package org.example.securitypractica.exception;

import io.minio.errors.ErrorResponseException;

public class StorageException extends RuntimeException {
    public StorageException(String message, Exception e) {
        super(message);
    }
}
