package com.careeragent.integration.portal.linkedin;

import com.careeragent.domain.Job;
import com.careeragent.domain.JobStatus;
import com.careeragent.infrastructure.config.EmailIngestionConfig;
import com.careeragent.integration.email.EmailListener;
import com.careeragent.integration.email.EmailParser;
import com.careeragent.integration.email.FetchedEmail;
import com.careeragent.integration.email.ParsedJobPosting;
import com.careeragent.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LinkedInEmailIngestionAdapter covering ingestion behavior and deduplication.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LinkedInEmailIngestionAdapter — Requirements 3.2, 3.3, 3.5, 3.6, 3.7")
class LinkedInEmailIngestionAdapterTest {

    @Mock private EmailListener emailListener;
    @Mock private EmailParser emailParser;
    @Mock private JobRepository jobRepository;
    @Mock private EmailIngestionConfig config;

    @InjectMocks private LinkedInEmailIngestionAdapter adapter;

    private static final UUID CANDIDATE_ID = UUID.randomUUID();

    @Test
    @DisplayName("ingestJobs returns empty when ingestion is disabled")
    void ingestJobs_disabled_returnsEmpty() {
        when(config.isEnabled()).thenReturn(false);

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(emailListener, emailParser, jobRepository);
    }

    @Test
    @DisplayName("ingestJobs returns empty when no emails found")
    void ingestJobs_noEmails_returnsEmpty() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(Collections.emptyList());

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(emailParser);
    }

    @Test
    @DisplayName("ingestJobs creates jobs from parsed postings")
    void ingestJobs_twoPostings_twoJobsSaved() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Job Alert", "<html>jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(List.of(
                new ParsedJobPosting("Engineer", "Google", "NYC", "https://linkedin.com/jobs/view/1"),
                new ParsedJobPosting("PM", "Meta", "London", "https://linkedin.com/jobs/view/2")));
        when(jobRepository.existsByPrimaryUrl(anyString())).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).hasSize(2);
        verify(jobRepository, times(2)).save(any(Job.class));
    }

    @Test
    @DisplayName("ingestJobs skips duplicates by URL")
    void ingestJobs_duplicateUrl_skipsOne() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", "<html>jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(List.of(
                new ParsedJobPosting("Engineer", "Google", "NYC", "https://linkedin.com/jobs/view/1"),
                new ParsedJobPosting("PM", "Meta", "London", "https://linkedin.com/jobs/view/2")));
        when(jobRepository.existsByPrimaryUrl("https://linkedin.com/jobs/view/1")).thenReturn(true);
        when(jobRepository.existsByPrimaryUrl("https://linkedin.com/jobs/view/2")).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).hasSize(1);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    @DisplayName("ingestJobs skips postings with null URL")
    void ingestJobs_nullUrl_skipped() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", "<html>jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(
                List.of(new ParsedJobPosting("Engineer", "Google", "NYC", null)));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestJobs skips postings with blank URL")
    void ingestJobs_blankUrl_skipped() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", "<html>jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(
                List.of(new ParsedJobPosting("Engineer", "Google", "NYC", "   ")));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestJobs handles email with no HTML content")
    void ingestJobs_nullHtmlContent_skipped() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", null)));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(emailParser);
    }

    @Test
    @DisplayName("ingestJobs handles email with zero parsed postings")
    void ingestJobs_zeroParsedPostings_noJobsCreated() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", "<html>no jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(Collections.emptyList());

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).isEmpty();
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestJobs sets correct fields on created Job")
    void ingestJobs_correctFieldsSet() {
        when(config.isEnabled()).thenReturn(true);
        when(emailListener.fetchAndExtractEmails()).thenReturn(
                List.of(new FetchedEmail("Alert", "<html>jobs</html>")));
        when(emailParser.parseLinkedInAlert(anyString())).thenReturn(
                List.of(new ParsedJobPosting("Staff Engineer", "Spotify", "Stockholm", "https://linkedin.com/jobs/view/42")));
        when(jobRepository.existsByPrimaryUrl(anyString())).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        adapter.ingestJobs(CANDIDATE_ID);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertThat(saved.getCandidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(saved.getTitle()).isEqualTo("Staff Engineer");
        assertThat(saved.getCompany()).isEqualTo("Spotify");
        assertThat(saved.getLocation()).isEqualTo("Stockholm");
        assertThat(saved.getPrimaryUrl()).isEqualTo("https://linkedin.com/jobs/view/42");
        assertThat(saved.getSourceUrls()).containsExactly("https://linkedin.com/jobs/view/42");
        assertThat(saved.getSourceTypes()).containsExactly("LINKEDIN_EMAIL");
        assertThat(saved.getPortalIdentifier()).isEqualTo("LINKEDIN");
        assertThat(saved.getStatus()).isEqualTo(JobStatus.NEW);
    }

    @Test
    @DisplayName("ingestJobs continues processing when one email fails")
    void ingestJobs_oneEmailFails_continuesProcessing() {
        when(config.isEnabled()).thenReturn(true);
        FetchedEmail goodEmail = new FetchedEmail("Good Alert", "<html>good</html>");
        FetchedEmail badEmail = new FetchedEmail("Bad Alert", "<html>bad</html>");
        when(emailListener.fetchAndExtractEmails()).thenReturn(List.of(badEmail, goodEmail));
        when(emailParser.parseLinkedInAlert("<html>bad</html>"))
                .thenThrow(new RuntimeException("Parse failure"));
        when(emailParser.parseLinkedInAlert("<html>good</html>"))
                .thenReturn(List.of(new ParsedJobPosting("Engineer", "Google", "NYC", "https://linkedin.com/jobs/view/99")));
        when(jobRepository.existsByPrimaryUrl(anyString())).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Job> result = adapter.ingestJobs(CANDIDATE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Engineer");
    }

    @Test
    @DisplayName("getSourceType returns LINKEDIN_EMAIL")
    void getSourceType_returnsLinkedInEmail() {
        assertThat(adapter.getSourceType()).isEqualTo("LINKEDIN_EMAIL");
    }
}
