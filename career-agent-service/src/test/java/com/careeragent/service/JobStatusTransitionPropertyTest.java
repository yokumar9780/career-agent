package com.careeragent.service;

import com.careeragent.domain.JobStatus;
import net.jqwik.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * **Validates: Requirements 10.1, 10.2, 10.6**
 */
@Label("Feature: career-agent, Property 13: Job Status Transition Validity")
class JobStatusTransitionPropertyTest {

    private final JobStatusService jobStatusService = new JobStatusService(null, null);

    @Property(tries = 500)
    @Label("Valid transitions do not throw, invalid transitions throw IllegalStateException")
    void validTransitionsAcceptedInvalidTransitionsRejected(
            @ForAll("jobStatus") JobStatus from,
            @ForAll("jobStatus") JobStatus to) {

        Set<JobStatus> validTargets = JobStatus.getValidTransitions(from);

        if (validTargets.contains(to)) {
            // Should not throw for valid transitions
            jobStatusService.validateTransition(from, to);
        } else {
            // Should throw IllegalStateException for invalid transitions
            assertThatThrownBy(() -> jobStatusService.validateTransition(from, to))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition from " + from + " to " + to);
        }
    }

    @Property(tries = 500)
    @Label("No status transitions to itself (non-reflexive)")
    void noStatusTransitionsToItself(@ForAll("jobStatus") JobStatus status) {
        Set<JobStatus> validTargets = JobStatus.getValidTransitions(status);
        assertThat(validTargets).doesNotContain(status);
    }

    @Property(tries = 500)
    @Label("Terminal statuses have no outgoing transitions")
    void terminalStatusesHaveNoTransitions(@ForAll("terminalStatus") JobStatus terminal) {
        Set<JobStatus> validTargets = JobStatus.getValidTransitions(terminal);
        assertThat(validTargets).isEmpty();
    }

    @Property(tries = 500)
    @Label("Non-terminal statuses have at least one valid transition")
    void nonTerminalStatusesHaveTransitions(@ForAll("nonTerminalStatus") JobStatus status) {
        Set<JobStatus> validTargets = JobStatus.getValidTransitions(status);
        assertThat(validTargets).isNotEmpty();
    }

    @Property(tries = 500)
    @Label("Invalid transition error message lists valid next statuses")
    void invalidTransitionErrorListsValidStatuses(
            @ForAll("jobStatus") JobStatus from,
            @ForAll("jobStatus") JobStatus to) {

        Set<JobStatus> validTargets = JobStatus.getValidTransitions(from);
        if (validTargets.contains(to)) {
            return; // skip valid transitions
        }

        assertThatThrownBy(() -> jobStatusService.validateTransition(from, to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Valid next statuses:");
    }

    // --- Generators ---

    @Provide
    Arbitrary<JobStatus> jobStatus() {
        return Arbitraries.of(JobStatus.values());
    }

    @Provide
    Arbitrary<JobStatus> terminalStatus() {
        // Terminal statuses: CLOSED, REJECTED, EXPIRED have no valid transitions
        return Arbitraries.of(JobStatus.CLOSED, JobStatus.REJECTED, JobStatus.EXPIRED);
    }

    @Provide
    Arbitrary<JobStatus> nonTerminalStatus() {
        // Statuses that have at least one valid transition defined in VALID_TRANSITIONS
        return Arbitraries.of(
                JobStatus.NEW, JobStatus.ANALYZED, JobStatus.MATCHED,
                JobStatus.SHORTLISTED, JobStatus.APPLICATION_PREPARED,
                JobStatus.READY_TO_APPLY, JobStatus.SUBMISSION_FAILED,
                JobStatus.APPLIED, JobStatus.INTERVIEW, JobStatus.OFFER,
                JobStatus.SKIPPED
        );
    }
}
