package com.careeragent.integration.storage;

import java.io.InputStream;
import java.util.UUID;

/**
 * Abstraction for object/document storage (MinIO, S3, Azure Blob, etc.).
 */
public interface ObjectStorageService {

    /**
     * Stores an object and returns the storage key.
     */
    String store(UUID candidateId, String filename, String contentType, InputStream inputStream, long size);

    /**
     * Retrieves an object as an InputStream.
     */
    InputStream retrieve(String storageKey);

    /**
     * Deletes a single object by its storage key.
     */
    void delete(String storageKey);

    /**
     * Deletes all objects for a candidate using prefix-based deletion.
     */
    void deleteAllForCandidate(UUID candidateId);

    /**
     * Generates a pre-signed URL for temporary direct download.
     */
    String getPresignedUrl(String storageKey, int expirySeconds);
}
