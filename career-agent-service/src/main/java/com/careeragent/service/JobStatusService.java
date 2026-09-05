package com.careeragent.service;

import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import com.careeragent.domain.JobStatusHistory;
import com.careeragent.repository.JobRepository;
import com.careeragent.repository.JobStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and applies job status transitions, recording history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobStatusService {

    private final JobRepository jobRepository;
    private final JobStatusHistoryRepository jobStatusHistoryRepository;

    /**
     * Validates that transitioning from one status to another is allowed.
     */
    public void validateTransition(JobStatus from, JobStatus to) {
        Set<JobStatus> validTargets = JobStatus.getValidTransitions(from);
        if (!validTargets.contains(to)) {
            String validList = validTargets.stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Invalid status transition from " + from + " to " + to
                            + ". Valid next statuses: [" + validList + "]");
        }
    }

    /**
     * Transitions a job to a new status, validates the transition, and records history.
     */
    @Transactional
    public Job transitionStatus(Job job, JobStatus newStatus, String reason) {
        JobStatus currentStatus = job.getStatus();
        validateTransition(currentStatus, newStatus);

        job.setStatus(newStatus);
        job.setStatusChangedAt(Instant.now());
        job = jobRepository.save(job);

        JobStatusHistory history = JobStatusHistory.builder()
                .jobId(job.getId())
                .fromStatus(currentStatus.name())
                .toStatus(newStatus.name())
                .reason(reason)
                .build();
        jobStatusHistoryRepository.save(history);

        log.debug("Job {} transitioned from {} to {} (reason: {})",
                job.getId(), currentStatus, newStatus, reason);

        return job;
    }
}
