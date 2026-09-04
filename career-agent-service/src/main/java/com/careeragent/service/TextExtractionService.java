package com.careeragent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts plain text content from PDF and DOCX documents.
 */
@Service
@Slf4j
public class TextExtractionService {

    /**
     * Extracts text from a PDF using Apache PDFBox.
     */
    public String extractFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extracts text from a DOCX using Apache POI.
     */
    public String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String text = extractor.getText();
            extractor.close();
            return text;
        }
    }

    /**
     * Routes to the correct extractor based on content type.
     */
    public String extract(String contentType, InputStream inputStream) throws IOException {
        return switch (contentType) {
            case "application/pdf" -> extractFromPdf(inputStream);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractFromDocx(inputStream);
            case "application/msword" -> extractFromDocx(inputStream);
            default -> throw new IllegalArgumentException("Unsupported file type: " + contentType);
        };
    }
}
