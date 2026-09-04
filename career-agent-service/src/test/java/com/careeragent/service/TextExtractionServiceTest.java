package com.careeragent.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TextExtractionService covering PDF and DOCX text extraction.
 */
@DisplayName("TextExtractionService — Requirements 1.1")
class TextExtractionServiceTest {

    private final TextExtractionService extractionService = new TextExtractionService();

    private byte[] createMinimalPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createMinimalDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("extractFromPdf extracts text from a valid PDF")
    void extractFromPdf_validPdf_extractsText() throws IOException {
        byte[] pdfBytes = createMinimalPdf("Senior Software Engineer Resume");
        String result = extractionService.extractFromPdf(new ByteArrayInputStream(pdfBytes));

        assertThat(result).contains("Senior Software Engineer Resume");
    }

    @Test
    @DisplayName("extractFromDocx extracts text from a valid DOCX")
    void extractFromDocx_validDocx_extractsText() throws IOException {
        byte[] docxBytes = createMinimalDocx("Full Stack Developer Experience");
        String result = extractionService.extractFromDocx(new ByteArrayInputStream(docxBytes));

        assertThat(result).contains("Full Stack Developer Experience");
    }

    @Test
    @DisplayName("extract routes to correct extractor based on content type")
    void extract_routesByContentType() throws IOException {
        byte[] pdfBytes = createMinimalPdf("PDF content");
        String pdfResult = extractionService.extract("application/pdf", new ByteArrayInputStream(pdfBytes));
        assertThat(pdfResult).contains("PDF content");

        byte[] docxBytes = createMinimalDocx("DOCX content");
        String docxResult = extractionService.extract(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new ByteArrayInputStream(docxBytes));
        assertThat(docxResult).contains("DOCX content");
    }

    @Test
    @DisplayName("extract throws IllegalArgumentException for unsupported content type")
    void extract_unsupportedType_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> extractionService.extract(
                "image/png", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }
}
