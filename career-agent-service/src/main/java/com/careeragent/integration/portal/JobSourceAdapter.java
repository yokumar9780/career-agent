package com.careeragent.integration.portal;

import com.careeragent.domain.Job;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction for ingesting jobs from a specific source type (email, career page, API).
 */
public interface JobSourceAdapter {

    /**
     * Returns the source type identifier for this adapter (e.g. LINKEDIN_EMAIL).
     */
    String getSourceType();

    /**
     * Ingests jobs for the given candidate and returns the raw job entities.
     */
    List<Job> ingestJobs(UUID candidateId);
}
