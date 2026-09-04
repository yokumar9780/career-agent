package com.careeragent.integration.storage;

/**
 * Thrown when an object storage operation fails.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
