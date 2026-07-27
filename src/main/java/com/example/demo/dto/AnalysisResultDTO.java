// dto/AnalysisResultDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisResultDTO {
    private String id;
    private String resumeId;
    private String jobDescriptionId;
    private Integer matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private ExperienceMatchDTO experienceMatch;
    private EducationMatchDTO educationMatch;
    private KeywordAnalysisDTO keywordAnalysis;
    private LocalDateTime analyzedAt;
}