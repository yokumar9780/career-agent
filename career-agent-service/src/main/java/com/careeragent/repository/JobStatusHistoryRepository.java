package com.careeragent.repository;

import com.careeragent.domain.JobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for JobStatusHistory entities.
 */
public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, UUID> {

    /**
     * Returns the status history for a job ordered chronologically.
     */
    List<JobStatusHistory> findByJobIdOrderByChangedAtAsc(UUID jobId);
}
