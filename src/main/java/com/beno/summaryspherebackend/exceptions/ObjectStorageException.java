package com.beno.summaryspherebackend.exceptions;

public class ObjectStorageException extends RuntimeException {
    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
