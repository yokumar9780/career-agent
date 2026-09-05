package com.careeragent.repository;

import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for Job entities.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Returns paginated jobs for a candidate.
     */
    Page<Job> findByCandidateId(UUID candidateId, Pageable pageable);

    /**
     * Returns paginated jobs for a candidate filtered by status.
     */
    Page<Job> findByCandidateIdAndStatus(UUID candidateId, JobStatus status, Pageable pageable);

    /**
     * Checks whether a job with the given primary URL already exists.
     */
    boolean existsByPrimaryUrl(String primaryUrl);

    /**
     * Finds a job by its primary URL.
     */
    Optional<Job> findByPrimaryUrl(String primaryUrl);

    /**
     * Finds a duplicate job by candidate, company, title, and location (case-insensitive).
     */
    Optional<Job> findByCandidateIdAndCompanyIgnoreCaseAndTitleIgnoreCaseAndLocationIgnoreCase(
            UUID candidateId, String company, String title, String location);
}
