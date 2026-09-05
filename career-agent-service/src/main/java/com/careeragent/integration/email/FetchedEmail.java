package com.careeragent.integration.email;

/**
 * Holds the pre-fetched content of an email message.
 */
public record FetchedEmail(String subject, String htmlContent) {
}
