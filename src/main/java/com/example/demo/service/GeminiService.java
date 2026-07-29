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
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    // The free Gemini tier used by this project caps requests at 10/minute.
    // "Run all checks" fires several calls back to back, which blows straight
    // through that limit and comes back as 429s (and sometimes 404s from Google
    // when the quota bucket is fully drained). This lock+timestamp pair forces
    // every call through this service to wait at least MIN_INTERVAL_MS since the
    // last one, so concurrent checks queue up instead of all firing at once.
    private static final long MIN_INTERVAL_MS = 6500; // ~9 req/min, safely under the 10 RPM cap
    private static final Object THROTTLE_LOCK = new Object();
    private static long lastCallTimestamp = 0L;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateContent(String prompt) {
        awaitThrottleSlot();
    System.out.println("================================");
System.out.println("Calling Gemini...");
System.out.println("Prompt length: " + prompt.length());
System.out.println("================================");
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
System.out.println("================================");
System.out.println("HTTP Status : " + e.getStatusCode());
System.out.println("Response Body:");
System.out.println(e.getResponseBodyAsString());
System.out.println("================================");
                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                if (retryable && attempt < MAX_RETRIES) {
                    attempt++;
                    sleepQuietly(RETRY_DELAY_MS * attempt);
                    continue;
                }
                throw new AnalysisException(buildFriendlyMessage(e), e);
} catch (Exception e) {
    e.printStackTrace();

    throw new AnalysisException(
        "AI service is temporarily unavailable. Please try again shortly.", e);
}
        }
    }

    private String buildFriendlyMessage(HttpStatusCodeException e) {
        if (e.getStatusCode().value() == 429) {
            return "AI service rate limit or daily quota exceeded. Please wait and try again later, "
                    + "or check the Gemini API plan/quota for this project.";
        }
        if (e.getStatusCode().value() == 404) {
            // On the free tier, Google sometimes returns 404 instead of 429 once a
            // project's quota bucket for this model is completely drained, rather
            // than a model-name problem. Surface that instead of a generic 404.
            return "AI service is unavailable, likely because the daily/per-minute Gemini quota for "
                    + "this project is exhausted. Check Rate Limit / Billing in Google AI Studio, or wait "
                    + "for the quota window to reset.";
        }
        return "AI service request failed (" + e.getStatusCode().value() + "). Please try again shortly.";
    }

    // Blocks the calling thread until at least MIN_INTERVAL_MS has passed since
    // the previous Gemini call made by ANY thread in this JVM. Synchronized so
    // concurrent "run all checks" requests serialize here instead of all
    // hitting Google in the same second.
    private void awaitThrottleSlot() {
        synchronized (THROTTLE_LOCK) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastCallTimestamp;
            if (elapsed < MIN_INTERVAL_MS) {
                sleepQuietly(MIN_INTERVAL_MS - elapsed);
            }
            lastCallTimestamp = System.currentTimeMillis();
        }
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
