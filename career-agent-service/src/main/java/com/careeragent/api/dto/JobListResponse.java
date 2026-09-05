package com.careeragent.api.dto;

import java.util.List;

/**
 * Paginated response wrapper for job listings.
 */
public record JobListResponse(
        List<JobResponse> jobs,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
