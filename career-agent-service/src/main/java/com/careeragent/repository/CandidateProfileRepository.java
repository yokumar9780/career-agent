package com.careeragent.repository;

import com.careeragent.domain.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for CandidateProfile entities.
 */
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByEmail(String email);

    boolean existsByEmail(String email);
}
