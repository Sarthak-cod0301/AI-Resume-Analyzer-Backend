// dto/InterviewSessionDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewSessionDTO {
    private String id;
    private String resumeId;
    private String jobDescriptionId;
    private String status;
    private List<InterviewQuestionDTO> questions;
    private List<AnswerEvaluationDTO> answers;
    private Double averageConfidence;
    private Double averageCorrectness;
    private Double averageCommunication;
    private Double averageTechnicalAccuracy;
    private Double overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}