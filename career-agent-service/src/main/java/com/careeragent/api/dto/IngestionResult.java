package com.careeragent.api.dto;

/**
 * Response DTO for job ingestion trigger results.
 */
public record IngestionResult(
        String status,
        String message
) {
}