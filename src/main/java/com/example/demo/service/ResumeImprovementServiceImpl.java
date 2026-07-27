// service/ResumeImprovementServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.ImprovementResponseDTO;
import com.example.demo.dto.SectionImprovementDTO;
import com.example.demo.entity.Resume;
import com.example.demo.entity.ResumeImprovement;
import com.example.demo.exception.ImprovementException;
import com.example.demo.repository.ResumeImprovementRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeImprovementServiceImpl implements ResumeImprovementService {

    private final ResumeRepository resumeRepository;
    private final ResumeImprovementRepository improvementRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public ImprovementResponseDTO improveResume(String resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ImprovementException("Resume not found or not owned by user"));

        String resumeText = textExtractionService.extractText(resume.getResumePath(), resume.getFileType());
        String prompt = buildPrompt(resumeText);

        String geminiResponse = geminiService.generateContent(prompt);
        List<SectionImprovementDTO> sections = parseGeminiResponse(geminiResponse);

        ResumeImprovement entity = ResumeImprovement.builder()
                .resumeId(resumeId)
                .userId(userId)
                .sections(sections)
                .generatedAt(LocalDateTime.now())
                .build();

        entity = improvementRepository.save(entity);
        return toDTO(entity);
    }

    private String buildPrompt(String resumeText) {
        return """
            You are an expert professional resume writer. Read the RESUME TEXT below, which was
            extracted from a PDF/DOCX file (formatting/line breaks may be imperfect - infer the
            original section boundaries as best you can).

            Identify these four sections if present: Summary/Objective, Experience, Projects, Skills.
            For each section that exists in the resume, rewrite it using stronger, more professional
            wording:
            - Replace weak/passive phrasing with strong action verbs (e.g. "worked on" -> "developed", "led", "engineered")
            - Add quantifiable impact where the original implies it - only quantify if there is a reasonable basis in the text, do NOT invent specific numbers/metrics not implied at all
            - Tighten wordy sentences, remove filler words
            - Keep all facts, technologies, company/project names EXACTLY as in the original - do not fabricate new experience
            - Preserve original meaning; only change phrasing/structure/impact framing

            Return ONLY a valid JSON array (no markdown, no extra text) with EXACTLY this structure,
            one entry per section found (skip sections that are entirely absent from the resume):

            [
              {
                "sectionName": "<Summary | Experience | Projects | Skills>",
                "originalContent": "<the original text of this section, as extracted>",
                "improvedContent": "<the rewritten, more professional version>",
                "changesSummary": "<one short sentence describing what kind of improvements were made>"
              }
            ]

            Rules:
            - Do NOT invent job titles, company names, dates, or technologies not present in the original.
            - Do NOT fabricate specific metrics/numbers that have no basis in the original text.
            - If a section is missing from the resume entirely, omit it from the array rather than inventing content.
            - Return ONLY the JSON array, nothing else.

            RESUME TEXT:
            %s
            """.formatted(resumeText);
    }

    private List<SectionImprovementDTO> parseGeminiResponse(String jsonResponse) {
        try {
            String cleaned = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<List<SectionImprovementDTO>>() {});
        } catch (Exception e) {
            throw new ImprovementException("Failed to parse AI improvement response", e);
        }
    }

    @Override
    public List<ImprovementResponseDTO> getHistory(String userId) {
        return improvementRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ImprovementResponseDTO getById(String id, String userId) {
        ResumeImprovement entity = improvementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ImprovementException("Improvement result not found"));
        return toDTO(entity);
    }

    private ImprovementResponseDTO toDTO(ResumeImprovement entity) {
        return ImprovementResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .sections(entity.getSections())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}