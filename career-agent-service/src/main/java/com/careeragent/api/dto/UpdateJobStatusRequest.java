package com.careeragent.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a job's status.
 */
public record UpdateJobStatusRequest(
        @NotNull String status,
        String reason
) {
}
