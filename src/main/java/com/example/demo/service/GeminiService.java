// service/GeminiService.java
package com.example.demo.service;

import com.example.demo.exception.AnalysisException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String primaryKey;

    @Value("${gemini.api.key.secondary}")
    private String secondaryKey;

    @Value("${gemini.api.key.tertiary}")
    private String tertiaryKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 4000;

    // --- Self-imposed rate limiter, per key pool ------------------------------------
    // Each ApiKeyPool is backed by a different Gemini API key (ideally from a
    // different Google Cloud project, so it has its own free-tier RPM/RPD quota
    // rather than sharing one). Features are split across pools so a burst on one
    // feature group doesn't exhaust the quota the others depend on. Within each pool
    // we still self-throttle to stay under that key's ~5 RPM cap.
    public enum ApiKeyPool { PRIMARY, SECONDARY, TERTIARY }

    private static final int MAX_REQUESTS_PER_WINDOW = 4;
    private static final long WINDOW_MILLIS = 60_000;

    private final Map<ApiKeyPool, Deque<Long>> requestTimestamps = new EnumMap<>(ApiKeyPool.class);
    private final Map<ApiKeyPool, Object> rateLimitLocks = new EnumMap<>(ApiKeyPool.class);

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        for (ApiKeyPool pool : ApiKeyPool.values()) {
            requestTimestamps.put(pool, new ArrayDeque<>());
            rateLimitLocks.put(pool, new Object());
        }
    }

    /** Existing callers keep working unchanged - defaults to the primary key/pool. */
    public String generateContent(String prompt) {
        return generateContent(prompt, ApiKeyPool.PRIMARY);
    }

    public String generateContent(String prompt, ApiKeyPool pool) {
        awaitRateLimitSlot(pool);

        String key = resolveKey(pool);
        String url = apiUrl + "?key=" + key;

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
                log.warn("Gemini API call failed on pool {} with status {}: {}",
                        pool, e.getStatusCode(), e.getResponseBodyAsString());

                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                if (retryable && attempt < MAX_RETRIES) {
                    attempt++;
                    sleepQuietly(RETRY_DELAY_MS * attempt);
                    continue;
                }
                throw new AnalysisException(buildFriendlyMessage(e), e);
            } catch (Exception e) {
                log.warn("Gemini API call failed on pool {} with a non-HTTP error", pool, e);
                throw new AnalysisException("AI service is temporarily unavailable. Please try again shortly.", e);
            }
        }
    }

    private String resolveKey(ApiKeyPool pool) {
        return switch (pool) {
            case PRIMARY -> primaryKey;
            case SECONDARY -> secondaryKey;
            case TERTIARY -> tertiaryKey;
        };
    }

    private void awaitRateLimitSlot(ApiKeyPool pool) {
        Object lock = rateLimitLocks.get(pool);
        Deque<Long> timestamps = requestTimestamps.get(pool);
        synchronized (lock) {
            while (true) {
                long now = System.currentTimeMillis();
                while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                    timestamps.pollFirst();
                }
                if (timestamps.size() < MAX_REQUESTS_PER_WINDOW) {
                    timestamps.addLast(now);
                    return;
                }
                long oldest = timestamps.peekFirst();
                long waitMillis = Math.max(WINDOW_MILLIS - (now - oldest) + 250, 250);
                try {
                    lock.wait(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
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
