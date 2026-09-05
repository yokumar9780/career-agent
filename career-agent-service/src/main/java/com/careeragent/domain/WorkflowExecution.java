package com.careeragent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a single run of the job discovery and matching workflow.
 */
@Entity
@Table(name = "workflow_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.RUNNING;

    @Column(name = "trigger_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    @Column(name = "jobs_ingested")
    @Builder.Default
    private Integer jobsIngested = 0;

    @Column(name = "duplicates_detected")
    @Builder.Default
    private Integer duplicatesDetected = 0;

    @Column(name = "jobs_matched")
    @Builder.Default
    private Integer jobsMatched = 0;

    @Column(name = "jobs_shortlisted")
    @Builder.Default
    private Integer jobsShortlisted = 0;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
    }
}
