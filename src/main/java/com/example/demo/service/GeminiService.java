// service/GeminiService.java
package com.example.demo.service;

import com.example.demo.exception.AnalysisException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // Small retry for transient throttling only - won't help if the daily quota
    // itself is exhausted, but recovers from short-lived per-minute rate limits.
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 4000;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateContent(String prompt) {
        String url = apiUrl + "?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{ Map.of("text", prompt) })
                },
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int attempt = 0;
        while (true) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                return extractTextFromResponse(response.getBody());
            } catch (HttpStatusCodeException e) {
                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                if (retryable && attempt < MAX_RETRIES) {
                    attempt++;
                    sleepQuietly(RETRY_DELAY_MS * attempt);
                    continue;
                }
                throw new AnalysisException(buildFriendlyMessage(e), e);
            } catch (Exception e) {
                throw new AnalysisException("AI service is temporarily unavailable. Please try again shortly.", e);
            }
        }
    }

    private String buildFriendlyMessage(HttpStatusCodeException e) {
        if (e.getStatusCode().value() == 429) {
            return "AI service rate limit or daily quota exceeded. Please wait and try again later, "
                    + "or check the Gemini API plan/quota for this project.";
        }
        return "AI service request failed (" + e.getStatusCode().value() + "). Please try again shortly.";
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractTextFromResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new AnalysisException("Failed to parse Gemini response", e);
        }
    }
}
