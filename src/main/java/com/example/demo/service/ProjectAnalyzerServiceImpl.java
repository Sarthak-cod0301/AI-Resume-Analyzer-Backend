// service/ProjectAnalyzerServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.ProjectAnalysisResponseDTO;
import com.example.demo.dto.ProjectInsightDTO;
import com.example.demo.entity.ProjectAnalysis;
import com.example.demo.entity.Resume;
import com.example.demo.exception.ProjectAnalysisException;
import com.example.demo.repository.ProjectAnalysisRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectAnalyzerServiceImpl implements ProjectAnalyzerService {

    private final ResumeRepository resumeRepository;
    private final ProjectAnalysisRepository projectAnalysisRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public ProjectAnalysisResponseDTO analyzeProjects(String resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ProjectAnalysisException("Resume not found or not owned by user"));

        String resumeText = textExtractionService.extractText(resume.getGridFsId(), resume.getFileType());
        String prompt = buildPrompt(resumeText);

        String geminiResponse = geminiService.generateContent(prompt);
        GeminiProjectsRaw raw = parseGeminiResponse(geminiResponse);

     if (raw.projects == null) {
    raw.projects = List.of();
}

if (raw.generalRecommendations == null) {
    raw.generalRecommendations = List.of(
            "AI project analysis is temporarily unavailable."
    );
}

        double avgScore = raw.projects.stream()
                .mapToInt(ProjectInsightDTO::getComplexityScore)
                .average()
                .orElse(0.0);

        ProjectAnalysis entity = ProjectAnalysis.builder()
                .resumeId(resumeId)
                .userId(userId)
                .totalProjectsFound(raw.projects.size())
                .overallProjectScore(Math.round(avgScore * 10.0) / 10.0)
                .projects(raw.projects)
                .generalRecommendations(raw.generalRecommendations)
                .analyzedAt(LocalDateTime.now())
                .build();

        entity = projectAnalysisRepository.save(entity);
        return toDTO(entity);
    }

    private String buildPrompt(String resumeText) {
        return """
            You are a senior technical interviewer and resume reviewer. Read the RESUME TEXT below
            and locate the PROJECTS section. For EACH individual project listed, analyze it
            independently on its own technical merit (not against any specific job description).

            Pay special attention to THIN/UNDERDEVELOPED project descriptions. For example, if a
            project is listed as just "Library Management" or "Student Portal" with little to no
            detail, flag it as too short and explicitly list what's missing from these standard
            elements: Authentication, REST APIs, Database, Role (the candidate's specific role),
            Technologies used, and Challenges faced/solved.

            For each project, evaluate:
            - What technologies/frameworks are used (from the description, not just a skills list)
            - How technically substantial/complex it sounds (architecture, scale, integrations)
            - Whether the description is too short/vague to be useful to a reviewer
            - Which of these standard elements are missing: Authentication, REST APIs, Database, Role, Technologies, Challenges
            - Whether it states measurable impact or outcomes (users, performance, scale, etc.)
            - Whether it mentions a live deployment link, GitHub repo, or demo
            - What's well done vs. what's missing or weak in how it's described

            Return ONLY a valid JSON object (no markdown, no extra text) with EXACTLY this structure:

            {
              "projects": [
                {
                  "projectName": "<name of project as written in resume>",
                  "technologiesUsed": [<list of technologies mentioned for this project>],
                  "complexityScore": <integer 0-100, technical substance/depth of this project>,
                  "complexityLevel": "<Beginner | Intermediate | Advanced>",
                  "descriptionTooShort": <true/false>,
                  "missingElements": [<subset of: Authentication, REST APIs, Database, Role, Technologies, Challenges - only ones genuinely missing>],
                  "strengths": [<what is described well, 1-3 items>],
                  "weaknesses": [<what is missing/weak, 1-3 items>],
                  "improvementTips": [<specific actionable rewrite suggestions, 1-3 items>],
                  "hasQuantifiableImpact": <true/false>,
                  "hasDeployedLink": <true/false>
                }
              ],
              "generalRecommendations": [<2-4 resume-wide tips about the projects section as a whole>]
            }

            Rules:
            - Only include projects that are actually distinct entries in the resume - do not split one project into multiple.
            - Base complexityScore on genuine technical signals in the text, not on how many buzzwords are used.
            - Do not invent technologies or outcomes not stated or clearly implied.
            - Return ONLY the JSON object, nothing else.

            RESUME TEXT:
            %s
            """.formatted(resumeText);
    }

private GeminiProjectsRaw parseGeminiResponse(String jsonResponse) {

    GeminiProjectsRaw fallback = new GeminiProjectsRaw();
    fallback.projects = List.of();
    fallback.generalRecommendations =
            List.of("AI project analysis is temporarily unavailable.");

    try {

        if(jsonResponse == null || jsonResponse.isBlank()) {
            return fallback;
        }

        String cleaned = jsonResponse
                .replace("```json","")
                .replace("```","")
                .trim();

        GeminiProjectsRaw raw =
                objectMapper.readValue(cleaned, GeminiProjectsRaw.class);

        if(raw.projects == null)
            raw.projects = List.of();

        if(raw.generalRecommendations == null)
            raw.generalRecommendations = List.of();

        return raw;

    } catch(Exception e) {

        return fallback;
    }
}

    @Override
    public List<ProjectAnalysisResponseDTO> getHistory(String userId) {
        return projectAnalysisRepository.findByUserIdOrderByAnalyzedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ProjectAnalysisResponseDTO getById(String id, String userId) {
        ProjectAnalysis entity = projectAnalysisRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ProjectAnalysisException("Project analysis not found"));
        return toDTO(entity);
    }

    private ProjectAnalysisResponseDTO toDTO(ProjectAnalysis entity) {
        return ProjectAnalysisResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .totalProjectsFound(entity.getTotalProjectsFound())
                .overallProjectScore(entity.getOverallProjectScore())
                .projects(entity.getProjects())
                .generalRecommendations(entity.getGeneralRecommendations())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }

    private static class GeminiProjectsRaw {
        public List<ProjectInsightDTO> projects;
        public List<String> generalRecommendations;
    }
}
