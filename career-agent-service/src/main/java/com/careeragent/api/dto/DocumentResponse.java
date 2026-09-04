package com.careeragent.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long fileSize,
        boolean primaryCv,
        String extractedText,
        Instant uploadedAt
) {
}
