package com.careeragent.service;

import com.careeragent.api.exception.ForbiddenException;
import com.careeragent.api.exception.ResourceNotFoundException;
import com.careeragent.api.exception.ValidationException;
import com.careeragent.domain.CandidateDocument;
import com.careeragent.integration.storage.ObjectStorageService;
import com.careeragent.repository.CandidateDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentService covering upload, listing, and deletion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService — Requirements 1.1, 2.3")
class DocumentServiceTest {

    @Mock private CandidateDocumentRepository documentRepository;
    @Mock private ObjectStorageService storageService;
    @Mock private TextExtractionService textExtractionService;

    @InjectMocks private DocumentService documentService;

    private MultipartFile mockFile(String filename, String contentType, long size) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        return file;
    }

    // --- uploadDocument ---

    @Test
    @DisplayName("uploadDocument succeeds for valid PDF")
    void uploadDocument_validPdf_succeeds() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("resume.pdf", "application/pdf", 1024L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(0L);
        when(storageService.store(eq(candidateId), any(), any(), any(), anyLong())).thenReturn("key/resume.pdf");
        when(textExtractionService.extract(any(), any())).thenReturn("Extracted text");
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateDocument result = documentService.uploadDocument(candidateId, file);

        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getStoragePath()).isEqualTo("key/resume.pdf");
        assertThat(result.getExtractedText()).isEqualTo("Extracted text");
    }

    @Test
    @DisplayName("uploadDocument succeeds for valid DOCX")
    void uploadDocument_validDocx_succeeds() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 2048L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(0L);
        when(storageService.store(eq(candidateId), any(), any(), any(), anyLong())).thenReturn("key/resume.docx");
        when(textExtractionService.extract(any(), any())).thenReturn("DOCX text");
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateDocument result = documentService.uploadDocument(candidateId, file);

        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).isEqualTo("DOCX text");
    }

    @Test
    @DisplayName("uploadDocument rejects invalid content type (e.g., text/plain)")
    void uploadDocument_invalidContentType_throwsValidationException() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("notes.txt", "text/plain", 100L);

        assertThatThrownBy(() -> documentService.uploadDocument(candidateId, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    @DisplayName("uploadDocument rejects file over 5MB")
    void uploadDocument_oversizedFile_throwsValidationException() throws IOException {
        UUID candidateId = UUID.randomUUID();
        long overFiveMb = 5L * 1024 * 1024 + 1;
        MultipartFile file = mockFile("big.pdf", "application/pdf", overFiveMb);

        assertThatThrownBy(() -> documentService.uploadDocument(candidateId, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    @DisplayName("uploadDocument rejects when 5 documents already exist")
    void uploadDocument_maxDocuments_throwsValidationException() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("resume.pdf", "application/pdf", 1024L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(5L);

        assertThatThrownBy(() -> documentService.uploadDocument(candidateId, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Maximum 5 documents");
    }

    @Test
    @DisplayName("uploadDocument marks first document as primary CV")
    void uploadDocument_firstDocument_markedAsPrimaryCv() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("resume.pdf", "application/pdf", 1024L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(0L);
        when(storageService.store(eq(candidateId), any(), any(), any(), anyLong())).thenReturn("key");
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateDocument result = documentService.uploadDocument(candidateId, file);

        assertThat(result.getPrimaryCv()).isTrue();
    }

    @Test
    @DisplayName("uploadDocument marks subsequent documents as non-primary")
    void uploadDocument_subsequentDocument_notPrimaryCv() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("cover.pdf", "application/pdf", 1024L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(1L);
        when(storageService.store(eq(candidateId), any(), any(), any(), anyLong())).thenReturn("key");
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateDocument result = documentService.uploadDocument(candidateId, file);

        assertThat(result.getPrimaryCv()).isFalse();
    }

    @Test
    @DisplayName("uploadDocument continues successfully when text extraction fails (best-effort)")
    void uploadDocument_extractionFails_continuesSuccessfully() throws IOException {
        UUID candidateId = UUID.randomUUID();
        MultipartFile file = mockFile("resume.pdf", "application/pdf", 1024L);
        when(documentRepository.countByCandidateId(candidateId)).thenReturn(0L);
        when(storageService.store(eq(candidateId), any(), any(), any(), anyLong())).thenReturn("key");
        when(textExtractionService.extract(any(), any())).thenThrow(new RuntimeException("Extraction error"));
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateDocument result = documentService.uploadDocument(candidateId, file);

        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).isNull();
        verify(documentRepository).save(any(CandidateDocument.class));
    }

    // --- listDocuments ---

    @Test
    @DisplayName("listDocuments returns all documents for candidate")
    void listDocuments_returnsAllDocuments() {
        UUID candidateId = UUID.randomUUID();
        List<CandidateDocument> docs = List.of(
                CandidateDocument.builder().candidateId(candidateId).filename("a.pdf").build(),
                CandidateDocument.builder().candidateId(candidateId).filename("b.pdf").build()
        );
        when(documentRepository.findByCandidateId(candidateId)).thenReturn(docs);

        List<CandidateDocument> result = documentService.listDocuments(candidateId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CandidateDocument::getFilename).containsExactly("a.pdf", "b.pdf");
    }

    // --- deleteDocument ---

    @Test
    @DisplayName("deleteDocument succeeds when candidate owns document")
    void deleteDocument_owner_succeeds() {
        UUID candidateId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        CandidateDocument doc = CandidateDocument.builder()
                .id(docId)
                .candidateId(candidateId)
                .filename("resume.pdf")
                .storagePath("key/resume.pdf")
                .build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(candidateId, docId);

        verify(storageService).delete("key/resume.pdf");
        verify(documentRepository).delete(doc);
    }

    @Test
    @DisplayName("deleteDocument throws ForbiddenException for non-owner")
    void deleteDocument_nonOwner_throwsForbiddenException() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        CandidateDocument doc = CandidateDocument.builder()
                .id(docId)
                .candidateId(ownerId)
                .filename("resume.pdf")
                .storagePath("key/resume.pdf")
                .build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.deleteDocument(attackerId, docId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("deleteDocument throws ResourceNotFoundException for non-existent document")
    void deleteDocument_notFound_throwsResourceNotFoundException() {
        UUID candidateId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(candidateId, docId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Document not found");
    }
}
