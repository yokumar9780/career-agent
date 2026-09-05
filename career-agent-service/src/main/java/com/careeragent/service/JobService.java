package com.careeragent.service;

import com.careeragent.api.exception.ForbiddenException;
import com.careeragent.api.exception.ResourceNotFoundException;
import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import com.careeragent.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business logic for querying and managing jobs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobStatusService jobStatusService;

    /**
     * Returns paginated jobs for a candidate, optionally filtered by status.
     */
    public Page<Job> getJobs(UUID candidateId, JobStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return jobRepository.findByCandidateIdAndStatus(candidateId, statusFilter, pageable);
        }
        return jobRepository.findByCandidateId(candidateId, pageable);
    }

    /**
     * Returns a single job by ID, verifying candidate ownership.
     */
    public Job getJob(UUID candidateId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getCandidateId().equals(candidateId)) {
            throw new ForbiddenException("Access denied to job: " + jobId);
        }

        return job;
    }

    /**
     * Updates a job's status after verifying ownership and transition validity.
     */
    public Job updateJobStatus(UUID candidateId, UUID jobId, JobStatus newStatus, String reason) {
        Job job = getJob(candidateId, jobId);
        return jobStatusService.transitionStatus(job, newStatus, reason);
    }
}
