package com.careeragent.api;

import com.careeragent.api.dto.IngestionResult;
import com.careeragent.api.dto.JobListResponse;
import com.careeragent.api.dto.JobResponse;
import com.careeragent.api.dto.UpdateJobStatusRequest;
import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import com.careeragent.service.JobIngestionService;
import com.careeragent.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * REST endpoints for job listing, detail, status updates, and ingestion.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobIngestionService jobIngestionService;

    /**
     * Returns a paginated list of jobs for the authenticated candidate.
     */
    @GetMapping
    public ResponseEntity<JobListResponse> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID candidateId = getCurrentCandidateId();
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "ingestedAt"));

        JobStatus statusFilter = status != null ? JobStatus.valueOf(status) : null;
        Page<Job> jobPage = jobService.getJobs(candidateId, statusFilter, pageable);

        JobListResponse response = new JobListResponse(
                jobPage.getContent().stream().map(this::toJobResponse).toList(),
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the full detail of a single job.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Job job = jobService.getJob(getCurrentCandidateId(), id);
        return ResponseEntity.ok(toJobResponse(job));
    }

    /**
     * Updates the status of a job.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<JobResponse> updateJobStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobStatusRequest request) {

        JobStatus newStatus = JobStatus.valueOf(request.status());
        Job job = jobService.updateJobStatus(getCurrentCandidateId(), id, newStatus, request.reason());
        return ResponseEntity.ok(toJobResponse(job));
    }

    /**
     * Triggers manual job ingestion asynchronously for the authenticated candidate.
     */
    @PostMapping("/ingest")
    public ResponseEntity<IngestionResult> triggerIngestion() {
        UUID candidateId = getCurrentCandidateId();
        jobIngestionService.ingestJobsAsync(candidateId);
        return ResponseEntity.accepted()
                .body(new IngestionResult("started", "Job ingestion started in background. Refresh the jobs list to see new jobs."));
    }

    /**
     * Extracts the candidate ID from the current security context.
     */
    private UUID getCurrentCandidateId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) Objects.requireNonNull(auth, "Authentication must not be null").getPrincipal();
    }

    /**
     * Converts a Job entity to a JobResponse DTO.
     */
    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getRemoteType(),
                job.getSalaryRange(),
                job.getDescription(),
                job.getRequirements(),
                job.getSkills(),
                job.getPrimaryUrl(),
                job.getSourceUrls(),
                job.getSourceTypes(),
                job.getPortalIdentifier(),
                job.getStatus().name(),
                job.getPostedDate(),
                job.getIngestedAt(),
                job.getStatusChangedAt()
        );
    }
}
