// dto/DashboardSummaryDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummaryDTO {
    private Long totalResumes;
    private Long totalAnalyses;
    private Double averageAtsScore;
    private Integer highestMatchScore;
    private RecentResumeDTO recentResume;
    private RecentJobDescriptionDTO recentJobDescription;

    private List<ScoreTrendPointDTO> resumeScoreTrend;
    private List<SkillGapItemDTO> skillGap;
    private List<AtsImprovementPointDTO> atsImprovement;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentResumeDTO {
        private String id;
        private String resumeName;
        private LocalDateTime uploadDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentJobDescriptionDTO {
        private String id;
        private String title;
        private String company;
        private LocalDateTime createdDate;
    }
}