package com.careeragent.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a single job posting.
 */
public record JobResponse(
        UUID id,
        String title,
        String company,
        String location,
        String remoteType,
        String salaryRange,
        String description,
        List<String> requirements,
        List<String> skills,
        String primaryUrl,
        List<String> sourceUrls,
        List<String> sourceTypes,
        String portalIdentifier,
        String status,
        LocalDate postedDate,
        Instant ingestedAt,
        Instant statusChangedAt
) {
}
