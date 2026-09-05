package com.careeragent.integration.portal.linkedin;

import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import com.careeragent.infrastructure.config.EmailIngestionConfig;
import com.careeragent.integration.email.EmailListener;
import com.careeragent.integration.email.EmailParser;
import com.careeragent.integration.email.FetchedEmail;
import com.careeragent.integration.email.ParsedJobPosting;
import com.careeragent.integration.portal.JobSourceAdapter;
import com.careeragent.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Ingests job postings from LinkedIn Job Alert emails via IMAP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkedInEmailIngestionAdapter implements JobSourceAdapter {

    private final EmailListener emailListener;
    private final EmailParser emailParser;
    private final JobRepository jobRepository;
    private final EmailIngestionConfig config;

    @Override
    public String getSourceType() {
        return "LINKEDIN_EMAIL";
    }

    /**
     * Fetches LinkedIn alert emails, parses job postings, deduplicates, and persists new jobs.
     */
    @Override
    public List<Job> ingestJobs(UUID candidateId) {
        if (!config.isEnabled()) {
            log.info("Email ingestion is disabled — skipping LinkedIn email ingestion");
            return Collections.emptyList();
        }

        List<FetchedEmail> emails = emailListener.fetchAndExtractEmails();
        if (emails.isEmpty()) {
            log.info("No unprocessed LinkedIn alert emails found");
            return Collections.emptyList();
        }

        List<Job> newJobs = new ArrayList<>();
        int duplicateCount = 0;

        for (FetchedEmail email : emails) {
            try {
                if (email.htmlContent() == null || email.htmlContent().isBlank()) {
                    log.warn("No HTML content found in email: {}", email.subject());
                    continue;
                }

                List<ParsedJobPosting> postings = emailParser.parseLinkedInAlert(email.htmlContent());
                if (postings.isEmpty()) {
                    log.warn("Zero job postings extracted from email: {}", email.subject());
                    continue;
                }

                for (ParsedJobPosting posting : postings) {
                    if (posting.url() == null || posting.url().isBlank()) {
                        log.warn("Skipping posting with no URL from email: {}", email.subject());
                        continue;
                    }

                    if (jobRepository.existsByPrimaryUrl(posting.url())) {
                        log.debug("Duplicate job skipped — URL already exists: {}", posting.url());
                        duplicateCount++;
                        continue;
                    }

                    Job job = Job.builder()
                            .candidateId(candidateId)
                            .title(posting.title())
                            .company(posting.company())
                            .location(posting.location())
                            .primaryUrl(posting.url())
                            .sourceUrls(List.of(posting.url()))
                            .sourceTypes(List.of("LINKEDIN_EMAIL"))
                            .portalIdentifier("LINKEDIN")
                            .status(JobStatus.NEW)
                            .build();

                    Job saved = jobRepository.save(job);
                    newJobs.add(saved);
                }

            } catch (Exception e) {
                log.error("Failed to process email: {}", email.subject(), e);
            }
        }

        log.info("Ingested {} new jobs from {} emails ({} duplicates skipped)",
                newJobs.size(), emails.size(), duplicateCount);

        return newJobs;
    }
}
