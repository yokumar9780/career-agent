package com.careeragent.api;

import com.careeragent.api.dto.DocumentResponse;
import com.careeragent.domain.CandidateDocument;
import com.careeragent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for document upload, listing, and deletion.
 */
@RestController
@RequestMapping("/api/v1/profiles/me/documents")
@Slf4j
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Uploads a document (PDF, DOC, or DOCX) with a 5MB size limit.
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        CandidateDocument doc = documentService.uploadDocument(getCurrentCandidateId(authentication), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDocumentResponse(doc));
    }

    /**
     * Lists all documents for the authenticated candidate.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(Authentication authentication) {
        List<CandidateDocument> docs = documentService.listDocuments(getCurrentCandidateId(authentication));
        return ResponseEntity.ok(docs.stream().map(this::toDocumentResponse).toList());
    }

    /**
     * Deletes a specific document by ID after ownership verification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id,
                                               Authentication authentication) {
        documentService.deleteDocument(getCurrentCandidateId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts the candidate ID from the authentication principal.
     */
    private UUID getCurrentCandidateId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Converts a CandidateDocument entity to a DocumentResponse DTO.
     */
    private DocumentResponse toDocumentResponse(CandidateDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getPrimaryCv(),
                doc.getExtractedText(),
                doc.getUploadedAt()
        );
    }
}
