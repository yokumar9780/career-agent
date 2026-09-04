package com.careeragent.service;

import net.jqwik.api.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Validates: Requirements 1.4**
 */
@Label("Feature: career-agent, Property 1: Profile Validation Completeness")
class ProfileValidationPropertyTest {

    /**
     * Mirrors the activation logic from ProfileService.updatePreferences():
     * canActivate = titles != null && !titles.isEmpty() && locations != null && !locations.isEmpty()
     */
    private boolean canActivate(List<String> titles, List<String> locations) {
        return titles != null && !titles.isEmpty()
                && locations != null && !locations.isEmpty();
    }

    @Property(tries = 200)
    @Label("Both non-empty titles and locations → active = true")
    void bothNonEmptyActivatesProfile(
            @ForAll("nonEmptyStringList") List<String> titles,
            @ForAll("nonEmptyStringList") List<String> locations) {
        assertThat(canActivate(titles, locations)).isTrue();
    }

    @Property(tries = 200)
    @Label("Empty titles → active = false")
    void emptyTitlesDeactivatesProfile(
            @ForAll("nonEmptyStringList") List<String> locations) {
        assertThat(canActivate(Collections.emptyList(), locations)).isFalse();
    }

    @Property(tries = 200)
    @Label("Empty locations → active = false")
    void emptyLocationsDeactivatesProfile(
            @ForAll("nonEmptyStringList") List<String> titles) {
        assertThat(canActivate(titles, Collections.emptyList())).isFalse();
    }

    @Property(tries = 100)
    @Label("Both empty → active = false")
    void bothEmptyDeactivatesProfile() {
        assertThat(canActivate(Collections.emptyList(), Collections.emptyList())).isFalse();
    }

    @Property(tries = 200)
    @Label("Null titles → active = false")
    void nullTitlesDeactivatesProfile(
            @ForAll("nonEmptyStringList") List<String> locations) {
        assertThat(canActivate(null, locations)).isFalse();
    }

    @Property(tries = 200)
    @Label("Null locations → active = false")
    void nullLocationsDeactivatesProfile(
            @ForAll("nonEmptyStringList") List<String> titles) {
        assertThat(canActivate(titles, null)).isFalse();
    }

    @Property(tries = 100)
    @Label("Both null → active = false")
    void bothNullDeactivatesProfile() {
        assertThat(canActivate(null, null)).isFalse();
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<String>> nonEmptyStringList() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .list()
                .ofMinSize(1)
                .ofMaxSize(10);
    }
}
