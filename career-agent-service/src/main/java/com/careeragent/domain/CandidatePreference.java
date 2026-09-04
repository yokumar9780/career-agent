package com.careeragent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores a candidate's job search preferences (titles, locations, salary, etc.).
 */
@Entity
@Table(name = "candidate_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private CandidateProfile candidate;

    @Column(name = "target_job_titles", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> targetJobTitles = new ArrayList<>();

    @Column(name = "preferred_locations", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();

    @Column(name = "remote_preferences", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> remotePreferences = new ArrayList<>();

    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "preferred_industries", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> preferredIndustries = new ArrayList<>();

    @Column(name = "target_companies", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> targetCompanies = new ArrayList<>();

    @Column(name = "seniority_levels", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> seniorityLevels = new ArrayList<>();

    @Column(name = "must_have_requirements", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> mustHaveRequirements = new ArrayList<>();

    @Column(name = "exclusions", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> exclusions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
