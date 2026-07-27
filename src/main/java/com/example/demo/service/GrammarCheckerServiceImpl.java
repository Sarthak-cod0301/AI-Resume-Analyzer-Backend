// service/GrammarCheckerServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.GrammarCheckResult;
import com.example.demo.entity.Resume;
import com.example.demo.exception.GrammarCheckException;
import com.example.demo.repository.GrammarCheckResultRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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

        String resumeText = textExtractionService.extractText(resume.getResumePath(), resume.getFileType());

        List<WeakWordIssueDTO> weakWordIssues = detectWeakWords(resumeText);
        List<PassiveVoiceIssueDTO> passiveVoiceIssues = detectPassiveVoice(resumeText);
        List<GrammarIssueDTO> grammarIssues = detectGrammarMistakesViaAI(resumeText);

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

    private List<PassiveVoiceIssueDTO> detectPassiveVoice(String text) {
        List<PassiveVoiceIssueDTO> issues = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?\\n])\\s+");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty() || trimmed.length() < 10) continue;

            Matcher m = PASSIVE_PATTERN.matcher(trimmed);
            if (m.find()) {
                issues.add(PassiveVoiceIssueDTO.builder()
                        .originalSentence(trimmed.length() > 150 ? trimmed.substring(0, 150) + "..." : trimmed)
                        .suggestedActiveSentence(null)
                        .build());
            }
        }
        return rewritePassiveSentencesViaAI(issues);
    }

    private List<PassiveVoiceIssueDTO> rewritePassiveSentencesViaAI(List<PassiveVoiceIssueDTO> detected) {
        if (detected.isEmpty()) return detected;

        List<PassiveVoiceIssueDTO> limited = detected.stream().limit(15).collect(Collectors.toList());

        String sentenceList = limited.stream()
                .map(PassiveVoiceIssueDTO::getOriginalSentence)
                .collect(Collectors.joining("\n- ", "- ", ""));

        String prompt = """
            Rewrite each of the following resume sentences from passive voice into active voice.
            Keep all facts, technologies, and meaning exactly the same - only change sentence structure.

            Return ONLY a valid JSON array (no markdown, no extra text) with EXACTLY this structure,
            in the SAME ORDER as the input sentences:

            [
              { "original": "<original sentence>", "active": "<active voice rewrite>" }
            ]

            SENTENCES:
            %s
            """.formatted(sentenceList);

        try {
            String response = geminiService.generateContent(prompt);
            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();
            List<Map<String, String>> rewrites = objectMapper.readValue(cleaned, new TypeReference<>() {});

            List<PassiveVoiceIssueDTO> result = new ArrayList<>();
            for (int i = 0; i < limited.size() && i < rewrites.size(); i++) {
                result.add(PassiveVoiceIssueDTO.builder()
                        .originalSentence(limited.get(i).getOriginalSentence())
                        .suggestedActiveSentence(rewrites.get(i).get("active"))
                        .build());
            }
            return result;
        } catch (Exception e) {
            return limited; // fall back to flagged-only if AI rewrite fails
        }
    }

    private List<GrammarIssueDTO> detectGrammarMistakesViaAI(String text) {
        String prompt = """
            You are a professional proofreader. Find GRAMMAR mistakes only (not style, not tone) in
            the resume text below - things like subject-verb agreement, tense inconsistency, wrong
            prepositions, punctuation errors, or spelling mistakes.

            Return ONLY a valid JSON array (no markdown, no extra text) with EXACTLY this structure:

            [
              {
                "originalText": "<the exact incorrect phrase/sentence as it appears>",
                "correctedText": "<the corrected version>",
                "explanation": "<brief explanation, e.g. 'Subject-verb agreement error'>"
              }
            ]

            Rules:
            - Only flag genuine grammar errors, not stylistic preferences.
            - Limit to the 15 most significant issues.
            - Return an empty array [] if no grammar mistakes are found.
            - Return ONLY the JSON array, nothing else.

            RESUME TEXT:
            %s
            """.formatted(text);

        try {
            String response = geminiService.generateContent(prompt);
            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<List<GrammarIssueDTO>>() {});
        } catch (Exception e) {
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
}