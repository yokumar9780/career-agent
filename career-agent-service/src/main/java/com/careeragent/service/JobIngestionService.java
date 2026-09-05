package com.careeragent.service;

import com.careeragent.domain.Job;
import com.careeragent.integration.portal.JobPortal;
import com.careeragent.integration.portal.JobSourceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates job ingestion from all registered portal source adapters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobIngestionService {

    private final List<JobPortal> jobPortals;

    /**
     * Ingests jobs for a candidate from all registered portals and source adapters.
     */
    public int ingestJobsForCandidate(UUID candidateId) {
        var adapterResults = jobPortals.stream()
                .flatMap(portal -> portal.getSourceAdapters().stream()
                        .map(adapter -> ingestFromAdapter(candidateId, portal, adapter)))
                .toList();

        int totalJobs = adapterResults.stream().mapToInt(Integer::intValue).sum();
        log.info("Ingested {} jobs from {} adapters across {} portals for candidate {}",
                totalJobs, adapterResults.size(), jobPortals.size(), candidateId);
        return totalJobs;
    }

    /**
     * Ingests jobs from a single adapter, returning the count or 0 on failure.
     */
    private int ingestFromAdapter(UUID candidateId, JobPortal portal, JobSourceAdapter adapter) {
        try {
            List<Job> jobs = adapter.ingestJobs(candidateId);
            int count = jobs != null ? jobs.size() : 0;
            log.debug("Adapter {} from portal {} ingested {} jobs for candidate {}",
                    adapter.getSourceType(), portal.getPortalIdentifier(), count, candidateId);
            return count;
        } catch (Exception e) {
            log.error("Failed to ingest jobs from adapter {} of portal {} for candidate {}",
                    adapter.getSourceType(), portal.getPortalIdentifier(), candidateId, e);
            return 0;
        }
    }

    /**
     * Runs job ingestion asynchronously using Spring's @Async thread pool.
     */
    @Async
    public void ingestJobsAsync(UUID candidateId) {
        log.info("Starting async job ingestion for candidate {}", candidateId);
        try {
            int count = ingestJobsForCandidate(candidateId);
            log.info("Async ingestion complete: {} jobs ingested for candidate {}", count, candidateId);
        } catch (Exception e) {
            log.error("Async job ingestion failed for candidate {}", candidateId, e);
        }
    }

    /**
     * Returns the list of registered portal identifiers for diagnostics.
     */
    public List<String> getRegisteredPortals() {
        return jobPortals.stream()
                .map(portal -> portal.getPortalIdentifier().name())
                .toList();
    }
}