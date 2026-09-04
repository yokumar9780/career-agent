package com.careeragent.service;

import com.careeragent.api.exception.ForbiddenException;
import com.careeragent.api.exception.ResourceNotFoundException;
import com.careeragent.api.exception.ValidationException;
import com.careeragent.domain.CandidateDocument;
import com.careeragent.integration.storage.ObjectStorageService;
import com.careeragent.repository.CandidateDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Manages document upload, listing, and deletion in MinIO storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final int MAX_DOCUMENTS_PER_PROFILE = 5;

    private final CandidateDocumentRepository documentRepository;
    private final ObjectStorageService storageService;
    private final TextExtractionService textExtractionService;

    /**
     * Uploads a document to storage, extracts text, and saves metadata.
     */
    public CandidateDocument uploadDocument(UUID candidateId, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new ValidationException("File type not allowed. Accepted: PDF, DOC, DOCX");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds 5MB limit");
        }

        long count = documentRepository.countByCandidateId(candidateId);
        if (count >= MAX_DOCUMENTS_PER_PROFILE) {
            throw new ValidationException("Maximum 5 documents per profile");
        }

        String storageKey = storageService.store(
                candidateId, file.getOriginalFilename(), file.getContentType(),
                file.getInputStream(), file.getSize());

        // Extract text (best-effort — don't fail upload if extraction fails)
        String extractedText = null;
        try {
            extractedText = textExtractionService.extract(contentType, file.getInputStream());
        } catch (Exception e) {
            log.warn("Text extraction failed for {}: {}", file.getOriginalFilename(), e.getMessage());
        }

        CandidateDocument doc = CandidateDocument.builder()
                .candidateId(candidateId)
                .filename(file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .storagePath(storageKey)
                .extractedText(extractedText)
                .primaryCv(count == 0) // First document is primary CV
                .build();
        doc = documentRepository.save(doc);

        log.info("Uploaded document '{}' for candidate {}", file.getOriginalFilename(), candidateId);
        return doc;
    }

    /**
     * Returns all documents for the given candidate.
     */
    public List<CandidateDocument> listDocuments(UUID candidateId) {
        return documentRepository.findByCandidateId(candidateId);
    }

    /**
     * Deletes a document after verifying ownership.
     */
    public void deleteDocument(UUID candidateId, UUID documentId) {
        CandidateDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getCandidateId().equals(candidateId)) {
            throw new ForbiddenException("You don't have permission to delete this document");
        }

        storageService.delete(doc.getStoragePath());
        documentRepository.delete(doc);

        log.info("Deleted document '{}' for candidate {}", doc.getFilename(), candidateId);
    }

    /**
     * Checks if the given content type is an allowed document format.
     */
    private boolean isAllowedContentType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/msword") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }
}
