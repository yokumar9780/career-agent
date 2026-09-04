package com.careeragent.api.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {
    }
}
