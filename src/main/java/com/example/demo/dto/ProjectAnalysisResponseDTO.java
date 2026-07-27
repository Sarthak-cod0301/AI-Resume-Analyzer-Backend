// dto/ProjectAnalysisResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectAnalysisResponseDTO {
    private String id;
    private String resumeId;
    private Integer totalProjectsFound;
    private Double overallProjectScore;
    private List<ProjectInsightDTO> projects;
    private List<String> generalRecommendations;
    private LocalDateTime analyzedAt;
}