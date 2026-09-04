package com.careeragent.service;

import com.careeragent.domain.RemotePreference;
import com.careeragent.domain.SeniorityLevel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides input sanitization and enum validation for candidate data.
 */
@Service
public class ValidationService {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * Strips HTML tags and script content from the input string.
     */
    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        String result = SCRIPT_TAG_PATTERN.matcher(input).replaceAll("");
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }

    /**
     * Sanitizes each string in a list, removing empty entries.
     */
    public List<String> sanitizeList(List<String> input) {
        if (input == null) {
            return null;
        }
        return input.stream()
                .map(this::sanitizeText)
                .filter(s -> s != null && !s.isEmpty())
                .toList();
    }

    /**
     * Validates and parses a seniority level string to its enum value.
     */
    public SeniorityLevel validateSeniorityLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        try {
            return SeniorityLevel.valueOf(level.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid seniority level: '" + level + "'. Must be one of: INTERN, JUNIOR, MID, SENIOR, LEAD, EXECUTIVE");
        }
    }

    /**
     * Validates and parses a remote preference string to its enum value.
     */
    public RemotePreference validateRemotePreference(String pref) {
        if (pref == null || pref.isBlank()) {
            return RemotePreference.ANY;
        }
        try {
            return RemotePreference.valueOf(pref.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid remote preference: '" + pref + "'. Must be one of: REMOTE, HYBRID, ON_SITE, ANY");
        }
    }

    /**
     * Validates that all values in the list are valid remote preferences.
     */
    public List<String> validateRemotePreferences(List<String> prefs) {
        if (prefs == null || prefs.isEmpty()) return Collections.emptyList();
        return prefs.stream()
                .map(p -> {
                    try {
                        return RemotePreference.valueOf(p.toUpperCase().trim()).name();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Invalid remote preference: '" + p + "'. Must be one of: REMOTE, HYBRID, ON_SITE, ANY");
                    }
                })
                .toList();
    }

    /**
     * Validates that all values in the list are valid seniority levels.
     */
    public List<String> validateSeniorityLevels(List<String> levels) {
        if (levels == null || levels.isEmpty()) return Collections.emptyList();
        return levels.stream()
                .map(l -> {
                    try {
                        return SeniorityLevel.valueOf(l.toUpperCase().trim()).name();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Invalid seniority level: '" + l + "'. Must be one of: INTERN, JUNIOR, MID, SENIOR, LEAD, EXECUTIVE");
                    }
                })
                .toList();
    }
}
