package com.careeragent.repository;

import com.careeragent.domain.CandidatePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for CandidatePreference entities.
 */
public interface CandidatePreferenceRepository extends JpaRepository<CandidatePreference, UUID> {
    Optional<CandidatePreference> findByCandidateId(UUID candidateId);

    void deleteByCandidateId(UUID candidateId);
}
