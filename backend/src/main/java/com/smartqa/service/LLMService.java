package com.smartqa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqa.exception.LLMException;
import com.smartqa.model.QAResponse;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class LLMService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${llm.model:claude-3-sonnet-20240229}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RetryRegistry retryRegistry;

    public LLMService() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(e -> shouldRetry(e))
                .build();
        this.retryRegistry = RetryRegistry.of(config);
    }

    /**
     * Ask a question about document content using Claude API with fallback to OpenAI
     */
    public QAResponse askQuestion(String documentId, String documentContent, 
                                   String question, int maxTokens) {
        long startTime = System.currentTimeMillis();
        String questionId = UUID.randomUUID().toString();

        try {
            // Try Claude first
            if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
                try {
                    return askClaudeWithRetry(documentId, documentContent, question, 
                                            maxTokens, questionId, startTime);
                } catch (Exception e) {
                    log.warn("Claude API failed, falling back to OpenAI: {}", e.getMessage());
                }
            }

            // Fallback to OpenAI
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                return askOpenAIWithRetry(documentId, documentContent, question, 
                                        maxTokens, questionId, startTime);
            }

            throw new LLMException("No API keys configured");

        } catch (Exception e) {
            log.error("Failed to answer question: {}", e.getMessage(), e);
            throw new LLMException("Failed to process question: " + e.getMessage(), e);
        }
    }

    private QAResponse askClaudeWithRetry(String documentId, String documentContent,
                                          String question, int maxTokens, 
                                          String questionId, long startTime) {
        Retry retry = retryRegistry.retry("claude-api");
        
        return Retry.decorateSupplier(retry, () -> 
            askClaude(documentId, documentContent, question, maxTokens, questionId, startTime)
        ).get();
    }

    private QAResponse askOpenAIWithRetry(String documentId, String documentContent,
                                         String question, int maxTokens,
                                         String questionId, long startTime) {
        Retry retry = retryRegistry.retry("openai-api");
        
        return Retry.decorateSupplier(retry, () ->
            askOpenAI(documentId, documentContent, question, maxTokens, questionId, startTime)
        ).get();
    }

    private QAResponse askClaude(String documentId, String documentContent,
                                String question, int maxTokens, String questionId, long startTime) {
        try {
            String prompt = buildPrompt(documentContent, question);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-3-sonnet-20240229");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));

            String responseText = callApiEndpoint(
                "https://api.anthropic.com/v1/messages",
                objectMapper.writeValueAsString(requestBody),
                "Claude-3",
                claudeApiKey
            );

            QAResponse response = parseClaudeResponse(responseText, questionId, question, 
                                                     documentId, startTime);
            response.setModel("claude-3-sonnet");
            return response;

        } catch (Exception e) {
            throw new LLMException("Claude API error: " + e.getMessage(), e);
        }
    }

    private QAResponse askOpenAI(String documentId, String documentContent,
                                String question, int maxTokens, String questionId, long startTime) {
        try {
            String prompt = buildPrompt(documentContent, question);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", 
                       "You are a helpful assistant that answers questions about documents."),
                Map.of("role", "user", "content", prompt)
            ));

            String responseText = callApiEndpoint(
                "https://api.openai.com/v1/chat/completions",
                objectMapper.writeValueAsString(requestBody),
                "Bearer",
                openaiApiKey
            );

            QAResponse response = parseOpenAIResponse(responseText, questionId, question,
                                                     documentId, startTime);
            response.setModel("gpt-3.5-turbo");
            return response;

        } catch (Exception e) {
            throw new LLMException("OpenAI API error: " + e.getMessage(), e);
        }
    }

    private String callApiEndpoint(String url, String body, String authType, String apiKey) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);

        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", authType + " " + apiKey);
        request.setEntity(new StringEntity(body));

        try {
            var response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode >= 400) {
                log.error("API error - Status: {}, Response: {}", statusCode, responseBody);
                throw new LLMException("API error: status " + statusCode);
            }

            return responseBody;
        } finally {
            client.close();
        }
    }

    private String buildPrompt(String documentContent, String question) {
        return String.format(
            "Based on the following document content:\n\n" +
            "---DOCUMENT START---\n%s\n---DOCUMENT END---\n\n" +
            "Answer the following question based ONLY on the content provided:\n" +
            "Question: %s\n\n" +
            "Provide a clear, concise answer. If the information is not in the document, " +
            "clearly state that.",
            documentContent, question
        );
    }

    private QAResponse parseClaudeResponse(String responseJson, String questionId,
                                         String question, String documentId, long startTime) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            String answer = (String) content.get(0).get("text");

            return QAResponse.builder()
                    .questionId(questionId)
                    .question(question)
                    .answer(answer)
                    .confidence(0.85)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .documentId(documentId)
                    .sources(List.of())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Claude response", e);
            throw new LLMException("Failed to parse Claude response", e);
        }
    }

    private QAResponse parseOpenAIResponse(String responseJson, String questionId,
                                         String question, String documentId, long startTime) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String answer = (String) message.get("content");

            return QAResponse.builder()
                    .questionId(questionId)
                    .question(question)
                    .answer(answer)
                    .confidence(0.82)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .documentId(documentId)
                    .sources(List.of())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new LLMException("Failed to parse OpenAI response", e);
        }
    }

    private static boolean shouldRetry(Throwable e) {
        // Retry on timeout, rate limits, and temporary errors
        return e.getMessage() != null && (
                e.getMessage().contains("timeout") ||
                e.getMessage().contains("429") ||
                e.getMessage().contains("temporary") ||
                e instanceof java.net.SocketTimeoutException
        );
    }
}
