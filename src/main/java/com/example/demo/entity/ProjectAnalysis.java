// entity/ProjectAnalysis.java
package com.example.demo.entity;

import com.example.demo.dto.ProjectInsightDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "project_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectAnalysis {

    @Id
    private String id;

    private String resumeId;
    private String userId;

    private Integer totalProjectsFound;
    private Double overallProjectScore;
    private List<ProjectInsightDTO> projects;      // embedded sub-documents
    private List<String> generalRecommendations;

    private LocalDateTime analyzedAt;
}