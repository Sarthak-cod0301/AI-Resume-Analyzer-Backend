// service/ResumeAnalysisServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.JobDescription;
import com.example.demo.entity.Resume;
import com.example.demo.entity.ResumeAnalysis;
import com.example.demo.exception.AnalysisException;
import com.example.demo.repository.JobDescriptionRepository;
import com.example.demo.repository.ResumeAnalysisRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public AnalysisResultDTO analyze(String resumeId, String jobDescriptionId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new AnalysisException("Resume not found or not owned by user"));

        JobDescription jd = jobDescriptionRepository.findByIdAndUserId(jobDescriptionId, userId)
                .orElseThrow(() -> new AnalysisException("Job description not found or not owned by user"));

        String resumeText = textExtractionService.extractText(resume.getGridFsId(), resume.getFileType());
        String prompt = buildPrompt(resumeText, jd.getDescription());

        String geminiJsonResponse = geminiService.generateContent(prompt);
        GeminiAnalysisRaw raw = parseGeminiResponse(geminiJsonResponse);

        ResumeAnalysis entity = ResumeAnalysis.builder()
                .resumeId(resumeId)
                .jobDescriptionId(jobDescriptionId)
                .userId(userId)
                .matchScore(raw.matchScore)
                .matchingSkills(raw.matchingSkills)
                .missingSkills(raw.missingSkills)
                .experienceMatch(raw.experienceMatch)
                .educationMatch(raw.educationMatch)
                .keywordAnalysis(raw.keywordAnalysis)
                .analyzedAt(LocalDateTime.now())
                .build();

        entity = analysisRepository.save(entity);
        return toDTO(entity);
    }

    private String buildPrompt(String resumeText, String jdText) {
        return """
            You are an expert technical recruiter and ATS system. Compare the RESUME against the
            JOB DESCRIPTION below and return ONLY a valid JSON object (no markdown, no extra text)
            with EXACTLY this structure:

            {
              "matchScore": <integer 0-100, overall fit percentage>,
              "matchingSkills": [<skills/technologies present in both resume and JD>],
              "missingSkills": [<skills/technologies required by JD but absent from resume>],
              "experienceMatch": {
                "requiredYears": <number, years of experience required by JD>,
                "foundYears": <number, years of experience found in resume>,
                "meetsRequirement": <true/false>
              },
              "educationMatch": {
                "requiredDegree": <string, degree required by JD, e.g. "Bachelor Degree">,
                "found": <true/false, whether resume meets this requirement>
              },
              "keywordAnalysis": {
                "keywordsPresent": [<important JD keywords found in resume>],
                "keywordsMissing": [<important JD keywords missing from resume>]
              }
            }

            Rules:
            - Base matchScore on skills overlap, experience match, and education match combined.
            - Keep skill/keyword names concise (e.g. "Spring Boot", not "Spring Boot framework experience").
            - Do not invent information not implied by either document.
            - Return ONLY the JSON object, nothing else.

            RESUME:
            %s

            JOB DESCRIPTION:
            %s
            """.formatted(resumeText, jdText);
    }

    private GeminiAnalysisRaw parseGeminiResponse(String jsonResponse) {
        try {
            String cleaned = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, GeminiAnalysisRaw.class);
        } catch (Exception e) {
            throw new AnalysisException("Failed to parse AI analysis response", e);
        }
    }

    @Override
    public List<AnalysisResultDTO> getHistory(String userId) {
        return analysisRepository.findByUserIdOrderByAnalyzedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AnalysisResultDTO getById(String id, String userId) {
        ResumeAnalysis entity = analysisRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AnalysisException("Analysis not found"));
        return toDTO(entity);
    }

    private AnalysisResultDTO toDTO(ResumeAnalysis entity) {
        return AnalysisResultDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .jobDescriptionId(entity.getJobDescriptionId())
                .matchScore(entity.getMatchScore())
                .matchingSkills(entity.getMatchingSkills())
                .missingSkills(entity.getMissingSkills())
                .experienceMatch(entity.getExperienceMatch())
                .educationMatch(entity.getEducationMatch())
                .keywordAnalysis(entity.getKeywordAnalysis())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }

    // Internal helper class matching Gemini's raw JSON structure
    private static class GeminiAnalysisRaw {
        public Integer matchScore;
        public List<String> matchingSkills;
        public List<String> missingSkills;
        public ExperienceMatchDTO experienceMatch;
        public EducationMatchDTO educationMatch;
        public KeywordAnalysisDTO keywordAnalysis;
    }
}
