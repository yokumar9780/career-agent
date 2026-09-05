package com.careeragent.integration.portal.linkedin;

import com.careeragent.domain.PortalType;
import com.careeragent.integration.portal.JobPortal;
import com.careeragent.integration.portal.JobSourceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Portal adapter for the LinkedIn job platform.
 */
@Component
@RequiredArgsConstructor
public class LinkedInPortalAdapter implements JobPortal {

    private final LinkedInEmailIngestionAdapter emailIngestionAdapter;

    @Override
    public PortalType getPortalIdentifier() {
        return PortalType.LINKEDIN;
    }

    /**
     * Returns all registered source adapters for LinkedIn (currently email ingestion only).
     */
    @Override
    public List<JobSourceAdapter> getSourceAdapters() {
        return List.of(emailIngestionAdapter);
    }

    /**
     * LinkedIn supports automated application submission via Easy Apply.
     */
    @Override
    public boolean supportsAutoApply() {
        return true;
    }
}
