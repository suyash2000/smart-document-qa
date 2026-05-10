package com.smartqa.service;

import com.smartqa.exception.DocumentProcessingException;
import com.smartqa.model.Document;
import com.smartqa.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdfdocument.PDFDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdfrw.PdfReader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DocumentProcessingService {

    private static final int CHUNK_SIZE_TOKENS = 800;
    private static final int CHUNK_OVERLAP_TOKENS = 160; // 20% overlap
    private static final double TOKENS_PER_CHARACTER = 0.25; // Rough estimate
    private static final Set<String> ALLOWED_TYPES = 
        Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
               "text/plain", "text/markdown");

    /**
     * Process uploaded file and create document with chunks
     */
    public Document processDocument(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        String documentId = UUID.randomUUID().toString();

        try {
            // Validate file
            validateFile(file);

            String fileName = file.getOriginalFilename();
            String fileType = getFileType(fileName);

            // Extract content based on file type
            String content = extractContent(file, fileType);

            // Create chunks with semantic awareness
            List<DocumentChunk> chunks = createIntelligentChunks(content);

            // Count pages (approximate)
            int pageCount = estimatePageCount(content);

            Document document = Document.builder()
                    .documentId(documentId)
                    .fileName(fileName)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .content(content)
                    .chunks(chunks)
                    .pageCount(pageCount)
                    .status(Document.DocumentStatus.PROCESSED)
                    .uploadedAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

            log.info("Document processed: {} - {} chunks created in {}ms",
                    fileName, chunks.size(), document.getProcessingTimeMs());

            return document;

        } catch (DocumentProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Document processing failed: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * Create intelligent chunks with semantic boundaries
     */
    private List<DocumentChunk> createIntelligentChunks(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // Split by sentences first
        String[] sentences = splitBySentences(content);

        int chunkIndex = 0;
        StringBuilder currentChunk = new StringBuilder();
        int currentPosition = 0;
        int startPosition = 0;

        for (String sentence : sentences) {
            int estimatedTokens = estimateTokens(currentChunk.toString());

            if (estimatedTokens > CHUNK_SIZE_TOKENS && !currentChunk.isEmpty()) {
                // Save current chunk
                chunks.add(DocumentChunk.builder()
                        .chunkIndex(chunkIndex++)
                        .content(currentChunk.toString().trim())
                        .startPosition(startPosition)
                        .endPosition(currentPosition)
                        .keywords(extractKeywords(currentChunk.toString()))
                        .build());

                // Start new chunk with overlap
                currentChunk = new StringBuilder();
                startPosition = currentPosition - (int)(CHUNK_OVERLAP_TOKENS / TOKENS_PER_CHARACTER);
            }

            currentChunk.append(sentence).append(" ");
            currentPosition += sentence.length() + 1;
        }

        // Add final chunk
        if (!currentChunk.isEmpty()) {
            chunks.add(DocumentChunk.builder()
                    .chunkIndex(chunkIndex)
                    .content(currentChunk.toString().trim())
                    .startPosition(startPosition)
                    .endPosition(currentPosition)
                    .keywords(extractKeywords(currentChunk.toString()))
                    .build());
        }

        return chunks;
    }

    /**
     * Extract content from file based on type
     */
    private String extractContent(MultipartFile file, String fileType) throws IOException {
        return switch (fileType) {
            case "pdf" -> extractPdfContent(file);
            case "docx" -> extractDocxContent(file);
            case "txt" -> new String(file.getBytes());
            case "md" -> new String(file.getBytes());
            default -> throw new DocumentProcessingException("Unsupported file type: " + fileType);
        };
    }

    private String extractPdfContent(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            PDFParser parser = new PDFParser(inputStream);
            PDFDocument document = parser.getPDDocument();
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        }
    }

    private String extractDocxContent(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            XWPFDocument document = new XWPFDocument(inputStream);
            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }

            return content.toString();
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DocumentProcessingException("File is empty");
        }

        if (file.getSize() > 50_000_000) { // 50MB limit
            throw new DocumentProcessingException("File exceeds 50MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new DocumentProcessingException("Unsupported file type: " + contentType);
        }
    }

    /**
     * Get file type from filename
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            throw new DocumentProcessingException("Invalid file name");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "pdf";
            case "docx" -> "docx";
            case "txt" -> "txt";
            case "md" -> "md";
            default -> throw new DocumentProcessingException("Unsupported file extension: " + extension);
        };
    }

    /**
     * Split content by sentences, preserving structure
     */
    private String[] splitBySentences(String content) {
        // Simple sentence splitter (can be improved with NLP)
        return content.split("(?<=[.!?])\\s+");
    }

    /**
     * Extract keywords from text
     */
    private List<String> extractKeywords(String text) {
        // Simple keyword extraction (can be enhanced)
        Set<String> keywords = new LinkedHashSet<>();
        String[] words = text.split("\\W+");

        for (String word : words) {
            if (word.length() > 5 && !isCommonWord(word)) {
                keywords.add(word.toLowerCase());
            }
        }

        return new ArrayList<>(keywords).stream().limit(5).toList();
    }

    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "which", "would", "could"
        );
        return commonWords.contains(word.toLowerCase());
    }

    /**
     * Estimate page count based on content length
     */
    private int estimatePageCount(String content) {
        // Roughly 3000 characters per page
        return Math.max(1, (content.length() / 3000));
    }

    /**
     * Estimate tokens in text (rough calculation)
     */
    private int estimateTokens(String text) {
        return (int) (text.length() * TOKENS_PER_CHARACTER);
    }

    /**
     * Get document from storage (in-memory for this example)
     */
    public Document getDocument(String documentId) {
        // This would fetch from database in production
        throw new DocumentProcessingException("Document not found: " + documentId);
    }
}
