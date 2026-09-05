package com.careeragent.integration.email;

import com.careeragent.infrastructure.config.EmailIngestionConfig;
import jakarta.mail.*;
import jakarta.mail.search.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Connects to an IMAP inbox and fetches unprocessed LinkedIn Job Alert emails.
 */
@Component
@Slf4j
public class EmailListener {

    private final String mailHost;
    private final int mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final EmailIngestionConfig config;

    /**
     * Explicit constructor required because @Value params are incompatible with @RequiredArgsConstructor (Rule 10).
     */
    public EmailListener(
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:993}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            EmailIngestionConfig config) {
        this.mailHost = mailHost;
        this.mailPort = mailPort;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.config = config;
    }

    /**
     * Fetches emails, extracts content while connection is open, marks as read, then closes.
     */
    public List<FetchedEmail> fetchAndExtractEmails() {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Mail host is not configured â€” skipping email fetch");
            return Collections.emptyList();
        }

        Store store = null;
        Folder folder = null;
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", mailHost);
            props.put("mail.imaps.host", mailHost);
            props.put("mail.imaps.port", String.valueOf(mailPort));

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(mailHost, mailPort, mailUsername, mailPassword);

            folder = store.getFolder(config.getFolder());
            folder.open(Folder.READ_WRITE);

            // Build search: unseen + from LinkedIn + received within lookback days
            SearchTerm unseenTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            SearchTerm fromTerm = new FromStringTerm(config.getLinkedinAlertSender());
            SearchTerm searchTerm = new AndTerm(unseenTerm, fromTerm);

            if (config.getLookbackDays() > 0) {
                Date sinceDate = Date.from(Instant.now().minus(config.getLookbackDays(), ChronoUnit.DAYS));
                SearchTerm dateTerm = new ReceivedDateTerm(jakarta.mail.search.ComparisonTerm.GE, sinceDate);
                searchTerm = new AndTerm(searchTerm, dateTerm);
                log.info("Filtering emails received in the last {} days (since {})",
                        config.getLookbackDays(), sinceDate);
            }

            Message[] messages = folder.search(searchTerm);
            int limit = Math.min(messages.length, config.getMaxEmailsPerRun());

            log.info("Found {} unprocessed LinkedIn alert emails in {} (processing up to {})",
                    messages.length, config.getFolder(), limit);

            List<FetchedEmail> result = new ArrayList<>(limit);

            for (int i = 0; i < limit; i++) {
                Message msg = messages[i];
                try {
                    String subject = msg.getSubject();
                    String html = extractHtmlContent(msg);

                    result.add(new FetchedEmail(subject, html));

                    // Mark as read while connection is still open
                    msg.setFlag(Flags.Flag.SEEN, true);

                } catch (Exception e) {
                    log.error("Failed to extract content from email at index {}: {}",
                            i, e.getMessage(), e);
                }
            }

            log.info("Extracted content from {} emails", result.size());
            return result;

        } catch (MessagingException e) {
            log.error("Failed to connect to email inbox: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            closeQuietly(folder, store);
        }
    }

    /**
     * Extracts HTML content from a Jakarta Mail message, handling multipart MIME.
     */
    private String extractHtmlContent(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String text) {
            return text;
        }

        if (content instanceof Multipart multipart) {
            return extractHtmlFromMultipart(multipart);
        }

        return null;
    }

    /**
     * Recursively searches multipart content for text/html parts.
     */
    private String extractHtmlFromMultipart(Multipart multipart) throws Exception {
        String htmlContent = null;
        String textContent = null;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String contentType = part.getContentType().toLowerCase();

            if (contentType.contains("text/html")) {
                htmlContent = (String) part.getContent();
            } else if (contentType.contains("text/plain") && textContent == null) {
                textContent = (String) part.getContent();
            } else if (contentType.contains("multipart")) {
                String nested = extractHtmlFromMultipart((Multipart) part.getContent());
                if (nested != null) {
                    htmlContent = nested;
                }
            }
        }

        return htmlContent != null ? htmlContent : textContent;
    }

    /**
     * Closes folder and store connections gracefully.
     */
    private void closeQuietly(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
        } catch (MessagingException e) {
            log.warn("Failed to close email folder: {}", e.getMessage());
        }
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            log.warn("Failed to close email store: {}", e.getMessage());
        }
    }
}
