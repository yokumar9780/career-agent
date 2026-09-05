package com.careeragent.integration.email;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses LinkedIn Job Alert email HTML to extract job postings.
 */
@Component
@Slf4j
public class EmailParser {

    private static final String JOBS_VIEW_PATTERN = "/jobs/view/";

    /**
     * Parses a LinkedIn Job Alert email and extracts individual job postings.
     */
    public List<ParsedJobPosting> parseLinkedInAlert(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            log.warn("Empty HTML content provided for parsing");
            return Collections.emptyList();
        }

        Document doc = Jsoup.parse(htmlContent);
        List<ParsedJobPosting> postings = new ArrayList<>();

        // Strategy 1: Find all links containing /jobs/view/ — the most reliable LinkedIn pattern
        Elements jobLinks = doc.select("a[href*=" + JOBS_VIEW_PATTERN + "]");

        for (Element link : jobLinks) {
            String url = cleanUrl(link.attr("href"));
            if (url == null || url.isBlank()) {
                continue;
            }

            String title = extractTitle(link);
            String company = extractCompany(link);
            String location = extractLocation(link);

            if (title.isBlank()) {
                log.debug("Skipping job link with empty title: {}", url);
                continue;
            }

            postings.add(new ParsedJobPosting(title, company, location, url));
        }

        // Strategy 2: If no /jobs/view/ links found, try broader job card selectors
        if (postings.isEmpty()) {
            postings = parseWithCardSelectors(doc);
        }

        return postings;
    }

    /**
     * Attempts parsing using common LinkedIn email job card container patterns.
     */
    private List<ParsedJobPosting> parseWithCardSelectors(Document doc) {
        List<ParsedJobPosting> postings = new ArrayList<>();

        // LinkedIn emails often use table-based layouts with job card containers
        String[] cardSelectors = {
                "table[role=presentation] a[href*=linkedin.com/jobs]",
                "td a[href*=linkedin.com/comm/jobs]",
                "a[href*=linkedin.com/comm/jobs/view]",
                "a[href*=linkedin.com/jobs/search]"
        };

        for (String selector : cardSelectors) {
            Elements elements = doc.select(selector);
            for (Element link : elements) {
                String url = cleanUrl(link.attr("href"));
                if (url == null || url.isBlank()) {
                    continue;
                }

                String title = extractTitle(link);
                if (title.isBlank()) {
                    continue;
                }

                String company = extractCompany(link);
                String location = extractLocation(link);

                postings.add(new ParsedJobPosting(title, company, location, url));
            }

            if (!postings.isEmpty()) {
                break;
            }
        }

        return postings;
    }

    /**
     * Extracts the job title from the link text or nearby elements.
     */
    private String extractTitle(Element link) {
        String text = link.text().trim();
        if (!text.isBlank()) {
            return text;
        }

        // Check for title in child elements
        Element titleElement = link.selectFirst("strong, b, span");
        if (titleElement != null) {
            return titleElement.text().trim();
        }

        return "";
    }

    /**
     * Extracts the company name from sibling or parent elements near the job link.
     */
    private String extractCompany(Element link) {
        // Check the parent container for company text
        Element parent = link.parent();
        if (parent != null) {
            // Look for siblings after the link that contain company info
            Element nextSibling = link.nextElementSibling();
            if (nextSibling != null) {
                String siblingText = nextSibling.text().trim();
                if (!siblingText.isBlank() && siblingText.length() < 200) {
                    return siblingText;
                }
            }

            // Try parent's parent for table-based layouts
            Element grandparent = parent.parent();
            if (grandparent != null) {
                Elements spans = grandparent.select("span, p, div");
                for (Element span : spans) {
                    if (span == link || span.selectFirst("a") != null) {
                        continue;
                    }
                    String text = span.ownText().trim();
                    if (!text.isBlank() && text.length() < 200 && !text.contains("http")) {
                        return text;
                    }
                }
            }
        }

        return "";
    }

    /**
     * Extracts the location from nearby elements in the job card.
     */
    private String extractLocation(Element link) {
        Element parent = link.parent();
        if (parent == null) {
            return "";
        }

        // LinkedIn emails sometimes include location in a separate line/element
        Element grandparent = parent.parent();
        if (grandparent != null) {
            Elements children = grandparent.select("span, p, div, td");
            boolean foundCompany = false;
            for (Element child : children) {
                if (child == link || child.selectFirst("a") != null) {
                    continue;
                }
                String text = child.ownText().trim();
                if (!text.isBlank() && text.length() < 200 && !text.contains("http")) {
                    if (foundCompany) {
                        return text;
                    }
                    foundCompany = true;
                }
            }
        }

        return "";
    }

    /**
     * Cleans a URL by removing tracking parameters and normalizing the base URL.
     */
    private String cleanUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String url = rawUrl.trim();

        // Remove common LinkedIn tracking query parameters
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }

        // Remove trailing slashes
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }
}
