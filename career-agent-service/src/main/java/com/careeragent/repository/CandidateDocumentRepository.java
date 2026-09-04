package com.careeragent.repository;

import com.careeragent.domain.CandidateDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for CandidateDocument entities.
 */
public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, UUID> {

    List<CandidateDocument> findByCandidateId(UUID candidateId);

    long countByCandidateId(UUID candidateId);

    void deleteByCandidateId(UUID candidateId);
}
