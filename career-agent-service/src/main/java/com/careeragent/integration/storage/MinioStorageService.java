package com.careeragent.integration.storage;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO-backed implementation of ObjectStorageService for document storage.
 */
@Service
@Slf4j
public class MinioStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Creates the storage bucket on startup if it does not already exist.
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to initialize MinIO bucket: " + bucketName, e);
        }
    }

    /**
     * Stores an object in MinIO and returns its storage key.
     */
    @Override
    public String store(UUID candidateId, String filename, String contentType, InputStream inputStream, long size) {
        String key = buildKey(candidateId, filename);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
            log.debug("Stored object: {}/{}", bucketName, key);
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to store object: " + key, e);
        }
    }

    /**
     * Retrieves an object from MinIO as an InputStream.
     */
    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve object: " + storageKey, e);
        }
    }

    /**
     * Deletes a single object from MinIO by its storage key.
     */
    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build());
            log.debug("Deleted object: {}/{}", bucketName, storageKey);
        } catch (Exception e) {
            throw new StorageException("Failed to delete object: " + storageKey, e);
        }
    }

    /**
     * Deletes all objects for a candidate using prefix-based listing.
     */
    @Override
    public void deleteAllForCandidate(UUID candidateId) {
        String prefix = candidateId.toString() + "/";
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            List<DeleteObject> toDelete = new LinkedList<>();
            for (Result<Item> result : objects) {
                toDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (!toDelete.isEmpty()) {
                var results = minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucketName)
                                .objects(toDelete)
                                .build());
                // Consume the results to trigger deletion
                for (var result : results) {
                    result.get(); // throws if individual delete failed
                }
            }
            log.debug("Deleted all objects for candidate {} ({} objects)", candidateId, toDelete.size());
        } catch (Exception e) {
            throw new StorageException("Failed to delete objects for candidate: " + candidateId, e);
        }
    }

    /**
     * Generates a pre-signed URL for temporary direct download access.
     */
    @Override
    public String getPresignedUrl(String storageKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(storageKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL for: " + storageKey, e);
        }
    }

    /**
     * Builds a sanitized storage key from the candidate ID and filename.
     */
    private String buildKey(UUID candidateId, String filename) {
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return candidateId.toString() + "/" + UUID.randomUUID() + "_" + sanitized;
    }
}
