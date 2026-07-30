// service/ResumeSuggestionServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.BulletSuggestionDTO;
import com.example.demo.dto.SuggestionResponseDTO;
import com.example.demo.entity.JobDescription;
import com.example.demo.entity.Resume;
import com.example.demo.entity.ResumeSuggestion;
import com.example.demo.exception.SuggestionException;
import com.example.demo.repository.JobDescriptionRepository;
import com.example.demo.repository.ResumeRepository;
import com.example.demo.repository.ResumeSuggestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeSuggestionServiceImpl implements ResumeSuggestionService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeSuggestionRepository suggestionRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public SuggestionResponseDTO generateSuggestions(String resumeId, String jobDescriptionId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new SuggestionException("Resume not found or not owned by user"));

        JobDescription jd = jobDescriptionRepository.findByIdAndUserId(jobDescriptionId, userId)
                .orElseThrow(() -> new SuggestionException("Job description not found or not owned by user"));

        String resumeText = textExtractionService.extractText(resume.getGridFsId(), resume.getFileType());
        String prompt = buildPrompt(resumeText, jd.getDescription());

        String geminiResponse = geminiService.generateContent(prompt, GeminiService.ApiKeyPool.TERTIARY);
        List<BulletSuggestionDTO> suggestions = parseGeminiResponse(geminiResponse);

        ResumeSuggestion entity = ResumeSuggestion.builder()
                .resumeId(resumeId)
                .jobDescriptionId(jobDescriptionId)
                .userId(userId)
                .suggestions(suggestions)
                .generatedAt(LocalDateTime.now())
                .build();

        entity = suggestionRepository.save(entity);
        return toDTO(entity);
    }

    private String buildPrompt(String resumeText, String jdText) {
        return """
            You are an expert resume writer and technical career coach. Compare the RESUME against
            the JOB DESCRIPTION below and produce TWO kinds of suggestions:

            1. MISSING_SKILL suggestions: identify skills/technologies the JD requires that are
               missing or weak in the resume. For each, write a realistic resume bullet the
               candidate COULD add, based ONLY on projects/experience already present in their
               resume (e.g. if they built a Spring Boot app, suggest how they could plausibly add
               Docker to containerize it). Do not invent unrelated experience.

            2. WEAK_BULLET_REPLACEMENT suggestions: find existing bullets in the resume that are
               vague or weakly worded (e.g. "Worked on APIs") and rewrite them into strong,
               specific, professional bullets (e.g. "Developed RESTful APIs using Spring Boot and
               Hibernate"). Keep the same underlying facts - only strengthen the wording.

            Return ONLY a valid JSON array (no markdown, no extra text) with EXACTLY this structure:

            [
              {
                "type": "MISSING_SKILL",
                "missingSkill": "<skill name, e.g. Docker>",
                "originalBullet": null,
                "suggestedBullet": "<a single realistic resume bullet, action-verb first, under 30 words>",
                "placementHint": "<which existing resume section/project this fits into>",
                "rationale": "<one short sentence explaining why this fits their existing background>"
              },
              {
                "type": "WEAK_BULLET_REPLACEMENT",
                "missingSkill": null,
                "originalBullet": "<the exact weak bullet as it appears in the resume>",
                "suggestedBullet": "<the stronger rewritten version>",
                "placementHint": "<which section this bullet is in>",
                "rationale": "<one short sentence on what was improved>"
              }
            ]

            Rules:
            - Only suggest bullets that are plausible extensions of the candidate's REAL existing projects/experience.
            - Do NOT fabricate companies, job titles, or unrelated technologies.
            - Limit to a combined total of the 5 most impactful suggestions across both types.
            - Each suggestedBullet must be specific and quantifiable where possible (avoid vague fluff).
            - Return ONLY the JSON array, nothing else.

            RESUME:
            %s

            JOB DESCRIPTION:
            %s
            """.formatted(resumeText, jdText);
    }

    private List<BulletSuggestionDTO> parseGeminiResponse(String jsonResponse) {
        try {
            String cleaned = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<List<BulletSuggestionDTO>>() {});
        } catch (Exception e) {
            throw new SuggestionException("Failed to parse AI suggestion response", e);
        }
    }

    @Override
    public List<SuggestionResponseDTO> getHistory(String userId) {
        return suggestionRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SuggestionResponseDTO getById(String id, String userId) {
        ResumeSuggestion entity = suggestionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new SuggestionException("Suggestion result not found"));
        return toDTO(entity);
    }

    private SuggestionResponseDTO toDTO(ResumeSuggestion entity) {
        return SuggestionResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .jobDescriptionId(entity.getJobDescriptionId())
                .suggestions(entity.getSuggestions())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}
