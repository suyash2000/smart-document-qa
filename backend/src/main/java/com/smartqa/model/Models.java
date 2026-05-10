package com.smartqa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String documentId;
    private String fileName;
    private String fileType; // pdf, docx, txt, md
    private long fileSize;
    private String content;
    private List<DocumentChunk> chunks;
    private int pageCount;
    private DocumentStatus status; // UPLOADED, PROCESSING, PROCESSED, FAILED
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private String errorMessage;
    private long processingTimeMs;

    public enum DocumentStatus {
        UPLOADED, PROCESSING, PROCESSED, FAILED
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DocumentChunk {
    private int chunkIndex;
    private String content;
    private int pageNumber;
    private int startPosition;
    private int endPosition;
    private double relevanceScore; // Set during search
    private List<String> keywords;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QARequest {
    private String documentId;
    private String question;
    private int maxTokens;
    private double temperatureValue; // 0.0 - 1.0
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAResponse {
    private String questionId;
    private String question;
    private String answer;
    private double confidence;
    private List<SourceChunk> sources;
    private long processingTimeMs;
    private String model;
    private String documentId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {
        private int chunkIndex;
        private String text;
        private double relevanceScore;
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    private String documentId;
    private String fileName;
    private int pageCount;
    private int chunks;
    private long processingTimeMs;
    private String status;
    private LocalDateTime uploadedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private long timestamp;
    private String path;
    private int statusCode;
}
