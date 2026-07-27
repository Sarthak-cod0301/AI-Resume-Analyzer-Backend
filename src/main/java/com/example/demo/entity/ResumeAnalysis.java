// entity/ResumeAnalysis.java
package com.example.demo.entity;

import com.example.demo.dto.EducationMatchDTO;
import com.example.demo.dto.ExperienceMatchDTO;
import com.example.demo.dto.KeywordAnalysisDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resume_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalysis {

    @Id
    private String id;

    private String resumeId;
    private String jobDescriptionId;
    private String userId;

    private Integer matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;

    private ExperienceMatchDTO experienceMatch;
    private EducationMatchDTO educationMatch;
    private KeywordAnalysisDTO keywordAnalysis;

    private LocalDateTime analyzedAt;
}