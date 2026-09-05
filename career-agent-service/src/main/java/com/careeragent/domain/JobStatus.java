package com.careeragent.domain;

import java.util.Map;
import java.util.Set;

/**
 * Represents the lifecycle status of a job posting.
 */
public enum JobStatus {
    NEW,
    ANALYZED,
    MATCHED,
    SHORTLISTED,
    APPLICATION_PREPARED,
    READY_TO_APPLY,
    APPLIED,
    INTERVIEW,
    OFFER,
    CLOSED,
    REJECTED,
    SKIPPED,
    EXPIRED,
    SUBMISSION_FAILED;

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(NEW, Set.of(ANALYZED, EXPIRED)),
            Map.entry(ANALYZED, Set.of(MATCHED, EXPIRED)),
            Map.entry(MATCHED, Set.of(SHORTLISTED, SKIPPED, EXPIRED)),
            Map.entry(SHORTLISTED, Set.of(APPLICATION_PREPARED, SKIPPED, EXPIRED)),
            Map.entry(APPLICATION_PREPARED, Set.of(READY_TO_APPLY, EXPIRED)),
            Map.entry(READY_TO_APPLY, Set.of(APPLIED, SUBMISSION_FAILED, EXPIRED)),
            Map.entry(SUBMISSION_FAILED, Set.of(READY_TO_APPLY)),
            Map.entry(APPLIED, Set.of(INTERVIEW, REJECTED)),
            Map.entry(INTERVIEW, Set.of(OFFER, REJECTED)),
            Map.entry(OFFER, Set.of(CLOSED, REJECTED)),
            Map.entry(SKIPPED, Set.of(SHORTLISTED))
    );

    /**
     * Returns the set of valid target statuses from the given status.
     */
    public static Set<JobStatus> getValidTransitions(JobStatus from) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of());
    }
}
