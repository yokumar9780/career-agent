package com.careeragent.repository;

import com.careeragent.domain.WorkflowExecution;
import com.careeragent.domain.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for WorkflowExecution entities.
 */
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    /**
     * Returns paginated workflow executions for a candidate, most recent first.
     */
    Page<WorkflowExecution> findByCandidateIdOrderByStartedAtDesc(UUID candidateId, Pageable pageable);

    /**
     * Checks whether a candidate has a workflow execution in the given status.
     */
    boolean existsByCandidateIdAndStatus(UUID candidateId, WorkflowStatus status);
}
