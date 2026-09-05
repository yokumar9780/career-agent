package com.careeragent.integration.portal;

import com.careeragent.domain.PortalType;

import java.util.List;

/**
 * Abstraction for an external job portal platform (LinkedIn, Indeed, etc.).
 */
public interface JobPortal {

    /**
     * Returns the identifier for this portal.
     */
    PortalType getPortalIdentifier();

    /**
     * Returns the list of source adapters this portal provides for job ingestion.
     */
    List<JobSourceAdapter> getSourceAdapters();

    /**
     * Indicates whether this portal supports automated application submission.
     */
    boolean supportsAutoApply();
}
