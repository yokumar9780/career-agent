package com.careeragent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "application_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationMode applicationMode = ApplicationMode.MANUAL;

    @Column(name = "pre_submit_review", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PreSubmitReview preSubmitReview = PreSubmitReview.ENABLED;

    @Column(name = "match_score_threshold", nullable = false)
    @Builder.Default
    private Integer matchScoreThreshold = 60;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

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
