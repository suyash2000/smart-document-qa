package com.smartqa.controller;

import com.smartqa.model.*;
import com.smartqa.service.DocumentProcessingService;
import com.smartqa.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class DocumentController {

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Autowired
    private LLMService llmService;

    // In-memory storage for demo (use database in production)
    private static final Map<String, Document> documentStore = new HashMap<>();

    /**
     * Upload and process document
     * POST /api/documents/upload
     */
    @PostMapping("/documents/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Processing document upload: {}", file.getOriginalFilename());

            // Process document
            Document document = documentProcessingService.processDocument(file);

            // Store document (in production, save to database)
            documentStore.put(document.getDocumentId(), document);

            // Return response
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                    .documentId(document.getDocumentId())
                    .fileName(document.getFileName())
                    .pageCount(document.getPageCount())
                    .chunks(document.getChunks().size())
                    .processingTimeMs(document.getProcessingTimeMs())
                    .status(document.getStatus().toString())
                    .uploadedAt(document.getUploadedAt())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Document upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get document details
     * GET /api/documents/{documentId}
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Document> getDocument(@PathVariable String documentId) {
        Document document = documentStore.get(documentId);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(document);
    }

    /**
     * List all documents
     * GET /api/documents
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<Document> documents = new ArrayList<>(documentStore.values());

        return ResponseEntity.ok(Map.of(
                "documents", documents,
                "totalCount", documents.size()
        ));
    }

    /**
     * Delete document
     * DELETE /api/documents/{documentId}
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        if (!documentStore.containsKey(documentId)) {
            return ResponseEntity.notFound().build();
        }

        documentStore.remove(documentId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document deleted successfully"
        ));
    }
}

@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QAController {

    @Autowired
    private LLMService llmService;

    // In-memory storage for QA history (use database in production)
    private static final Map<String, List<QAResponse>> qaHistory = new HashMap<>();
    private static final Map<String, Document> documentStore = new HashMap<>();

    /**
     * Ask question about document
     * POST /api/qa/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody QARequest request) {
        try {
            log.info("Processing question for document: {} - Question: {}", 
                    request.getDocumentId(), request.getQuestion());

            // Validate request
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Question cannot be empty"
                ));
            }

            // Get document (from in-memory store)
            Document document = documentStore.get(request.getDocumentId());
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Ask question to LLM
            int maxTokens = request.getMaxTokens() > 0 ? request.getMaxTokens() : 500;
            QAResponse response = llmService.askQuestion(
                    request.getDocumentId(),
                    document.getContent(),
                    request.getQuestion(),
                    maxTokens
            );

            // Store in history
            qaHistory.computeIfAbsent(request.getDocumentId(), k -> new ArrayList<>()).add(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Question processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to process question",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get QA history for document
     * GET /api/qa/history/{documentId}
     */
    @GetMapping("/history/{documentId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String documentId) {
        List<QAResponse> history = qaHistory.getOrDefault(documentId, new ArrayList<>());

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "history", history,
                "totalQuestions", history.size()
        ));
    }

    /**
     * Clear QA history
     * DELETE /api/qa/history/{documentId}
     */
    @DeleteMapping("/history/{documentId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String documentId) {
        qaHistory.remove(documentId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "History cleared successfully"
        ));
    }
}

/**
 * Global exception handler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unhandled exception", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .error("Internal Server Error")
                        .message(e.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .statusCode(500)
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .error("Bad Request")
                        .message(e.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .statusCode(400)
                        .build()
        );
    }
}
