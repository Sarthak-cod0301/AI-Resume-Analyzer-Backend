// service/GrammarCheckerServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.GrammarCheckResult;
import com.example.demo.entity.Resume;
import com.example.demo.exception.GrammarCheckException;
import com.example.demo.repository.GrammarCheckResultRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrammarCheckerServiceImpl implements GrammarCheckerService {

    private final ResumeRepository resumeRepository;
    private final GrammarCheckResultRepository grammarCheckResultRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> WEAK_WORD_MAP = new LinkedHashMap<>();
    static {
        WEAK_WORD_MAP.put("worked on", "Developed");
        WEAK_WORD_MAP.put("helped with", "Contributed to");
        WEAK_WORD_MAP.put("responsible for", "Managed");
        WEAK_WORD_MAP.put("involved in", "Drove");
        WEAK_WORD_MAP.put("did", "Executed");
        WEAK_WORD_MAP.put("made", "Built");
        WEAK_WORD_MAP.put("handled", "Directed");
        WEAK_WORD_MAP.put("dealt with", "Resolved");
        WEAK_WORD_MAP.put("assisted", "Supported");
        WEAK_WORD_MAP.put("participated in", "Contributed to");
        WEAK_WORD_MAP.put("tried to", "Pursued");
        WEAK_WORD_MAP.put("good", "Strong");
        WEAK_WORD_MAP.put("things", "Components");
        WEAK_WORD_MAP.put("stuff", "Deliverables");
        WEAK_WORD_MAP.put("very", "Significantly");
        WEAK_WORD_MAP.put("got", "Achieved");
        WEAK_WORD_MAP.put("worked as", "Served as");
    }

    private static final Pattern PASSIVE_PATTERN = Pattern.compile(
            "\\b(is|are|was|were|be|been|being)\\s+(\\w+ed|built|written|done|made|given|taken|known|shown|led)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public GrammarCheckResponseDTO runCheck(String resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new GrammarCheckException("Resume not found or not owned by user"));

        String resumeText = textExtractionService.extractText(resume.getGridFsId(), resume.getFileType());

        List<WeakWordIssueDTO> weakWordIssues = detectWeakWords(resumeText);
        List<String> passiveSentences = detectPassiveVoiceSentences(resumeText);

        // Single combined Gemini call covering both grammar mistakes and passive-voice
        // rewrites, instead of two separate calls - halves this check's AI quota usage.
        CombinedAiReviewRaw aiResult = runCombinedAiReview(resumeText, passiveSentences);
        List<GrammarIssueDTO> grammarIssues = aiResult.grammarIssues != null ? aiResult.grammarIssues : new ArrayList<>();
        List<PassiveVoiceIssueDTO> passiveVoiceIssues = buildPassiveVoiceIssues(passiveSentences, aiResult.passiveRewrites);

        int totalIssues = weakWordIssues.size() + passiveVoiceIssues.size() + grammarIssues.size();
        int score = calculateScore(resumeText, totalIssues);

        GrammarCheckResult entity = GrammarCheckResult.builder()
                .resumeId(resumeId)
                .userId(userId)
                .overallScore(score)
                .grammarIssues(grammarIssues)
                .weakWordIssues(weakWordIssues)
                .passiveVoiceIssues(passiveVoiceIssues)
                .totalIssuesFound(totalIssues)
                .checkedAt(LocalDateTime.now())
                .build();

        entity = grammarCheckResultRepository.save(entity);
        return toDTO(entity);
    }

    private List<WeakWordIssueDTO> detectWeakWords(String text) {
        List<WeakWordIssueDTO> issues = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?\\n])\\s+");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            for (Map.Entry<String, String> entry : WEAK_WORD_MAP.entrySet()) {
                Pattern p = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(trimmed);
                if (m.find()) {
                    issues.add(WeakWordIssueDTO.builder()
                            .weakWord(capitalizeMatch(m.group()))
                            .suggestedWord(entry.getValue())
                            .context(trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed)
                            .build());
                }
            }
        }
        return issues.stream().distinct().limit(30).collect(Collectors.toList());
    }

    private String capitalizeMatch(String match) {
        if (match.isEmpty()) return match;
        return Character.toUpperCase(match.charAt(0)) + match.substring(1);
    }

    // Regex-only pass - no AI call, just flags candidate sentences for the AI step below.
    private List<String> detectPassiveVoiceSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] split = text.split("(?<=[.!?\\n])\\s+");

        for (String sentence : split) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty() || trimmed.length() < 10) continue;

            Matcher m = PASSIVE_PATTERN.matcher(trimmed);
            if (m.find()) {
                sentences.add(trimmed.length() > 150 ? trimmed.substring(0, 150) + "..." : trimmed);
            }
        }
        return sentences.stream().limit(15).collect(Collectors.toList());
    }

    private List<PassiveVoiceIssueDTO> buildPassiveVoiceIssues(List<String> detectedSentences, List<PassiveRewriteRaw> rewrites) {
        List<PassiveVoiceIssueDTO> result = new ArrayList<>();
        for (int i = 0; i < detectedSentences.size(); i++) {
            String active = (rewrites != null && i < rewrites.size()) ? rewrites.get(i).active : null;
            result.add(PassiveVoiceIssueDTO.builder()
                    .originalSentence(detectedSentences.get(i))
                    .suggestedActiveSentence(active)
                    .build());
        }
        return result;
    }

    // Single Gemini call that covers both grammar-mistake detection AND passive-voice
    // rewrites in one round trip (previously two separate calls).
    private CombinedAiReviewRaw runCombinedAiReview(String text, List<String> passiveSentences) {
        String passiveSection = passiveSentences.isEmpty()
                ? "None detected."
                : passiveSentences.stream().collect(Collectors.joining("\n- ", "- ", ""));

        String prompt = """
            You are a professional proofreader and resume writing coach. You have two tasks on the
            resume text below.

            TASK 1 - GRAMMAR: Find GRAMMAR mistakes only (not style, not tone) in the FULL RESUME
            TEXT - things like subject-verb agreement, tense inconsistency, wrong prepositions,
            punctuation errors, or spelling mistakes. Limit to the 15 most significant issues.

            TASK 2 - PASSIVE VOICE REWRITES: For EACH sentence listed under PASSIVE VOICE SENTENCES
            below, rewrite it from passive voice into active voice. Keep all facts, technologies,
            and meaning exactly the same - only change sentence structure. Return exactly one
            rewrite per listed sentence, in the SAME ORDER as listed.

            Return ONLY a valid JSON object (no markdown, no extra text) with EXACTLY this structure:

            {
              "grammarIssues": [
                {
                  "originalText": "<the exact incorrect phrase/sentence as it appears>",
                  "correctedText": "<the corrected version>",
                  "explanation": "<brief explanation, e.g. 'Subject-verb agreement error'>"
                }
              ],
              "passiveRewrites": [
                { "original": "<original sentence>", "active": "<active voice rewrite>" }
              ]
            }

            Rules:
            - grammarIssues: only flag genuine grammar errors, not stylistic preferences. Return [] if none found.
            - passiveRewrites: return exactly one entry per sentence listed below, same order. Return [] if none listed.
            - Return ONLY the JSON object, nothing else.

            FULL RESUME TEXT:
            %s

            PASSIVE VOICE SENTENCES:
            %s
            """.formatted(text, passiveSection);

        String response;
        try {
            // Only the Gemini call itself is isolated here. If it fails (quota/rate
            // limit/etc.), GeminiService already throws a clear AnalysisException with
            // the real reason - don't swallow that behind a generic message below.
            response = geminiService.generateContent(prompt);
        } catch (Exception e) {
            // Degrade gracefully instead of failing the whole grammar check: weak-word
            // and passive-voice detection above are pure regex and still work fine even
            // when the AI quota is exhausted, so return those with no AI-detected grammar
            // issues rather than erroring out entirely (mirrors FormattingCheckerServiceImpl).
            CombinedAiReviewRaw fallback = new CombinedAiReviewRaw();
            fallback.grammarIssues = new ArrayList<>();
            fallback.passiveRewrites = null;
            return fallback;
        }

        try {
            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, CombinedAiReviewRaw.class);
        } catch (Exception e) {
            // This means Gemini responded successfully but the body wasn't valid JSON
            // (e.g. truncated output, or it added commentary despite instructions) -
            // this is genuinely a parse failure, so keep this message specific to that.
            throw new GrammarCheckException("Failed to parse AI grammar check response", e);
        }
    }

    private int calculateScore(String text, int totalIssues) {
        int wordCount = text.trim().isEmpty() ? 1 : text.trim().split("\\s+").length;
        double issueDensity = (totalIssues * 100.0) / Math.max(wordCount, 1);
        int score = (int) Math.round(100 - (issueDensity * 8));
        return Math.max(0, Math.min(100, score));
    }

    @Override
    public List<GrammarCheckResponseDTO> getHistory(String userId) {
        return grammarCheckResultRepository.findByUserIdOrderByCheckedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public GrammarCheckResponseDTO getById(String id, String userId) {
        GrammarCheckResult entity = grammarCheckResultRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GrammarCheckException("Grammar check result not found"));
        return toDTO(entity);
    }

    private GrammarCheckResponseDTO toDTO(GrammarCheckResult entity) {
        return GrammarCheckResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .overallScore(entity.getOverallScore())
                .grammarIssues(entity.getGrammarIssues())
                .weakWordIssues(entity.getWeakWordIssues())
                .passiveVoiceIssues(entity.getPassiveVoiceIssues())
                .totalIssuesFound(entity.getTotalIssuesFound())
                .checkedAt(entity.getCheckedAt())
                .build();
    }

    // Internal helpers matching the merged Gemini JSON response shape
    private static class PassiveRewriteRaw {
        public String original;
        public String active;
    }

    private static class CombinedAiReviewRaw {
        public List<GrammarIssueDTO> grammarIssues;
        public List<PassiveRewriteRaw> passiveRewrites;
    }
}
