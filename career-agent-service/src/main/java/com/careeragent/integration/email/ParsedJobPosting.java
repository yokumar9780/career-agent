package com.careeragent.integration.email;

/**
 * Represents a job posting extracted from a LinkedIn Job Alert email.
 */
public record ParsedJobPosting(String title, String company, String location, String url) {
}
