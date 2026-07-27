// dto/ProjectInsightDTO.java
package com.example.demo.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectInsightDTO {
    private String projectName;
    private List<String> technologiesUsed;
    private Integer complexityScore;         // 0-100
    private String complexityLevel;          // "Beginner", "Intermediate", "Advanced"
    private Boolean descriptionTooShort;     // flags thin descriptions like "Library Management"
    private List<String> missingElements;    // e.g. ["Authentication", "REST APIs", "Database", "Role", "Technologies", "Challenges"]
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvementTips;
    private Boolean hasQuantifiableImpact;
    private Boolean hasDeployedLink;
}