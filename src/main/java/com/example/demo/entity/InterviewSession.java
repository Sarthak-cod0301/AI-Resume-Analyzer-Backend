// entity/InterviewSession.java
package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "interview_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewSession {

    @Id
    private String id;

    private String resumeId;
    private String jobDescriptionId; // nullable - interview can be generic

    private String userId;

    private String status; // "IN_PROGRESS", "COMPLETED"

    private Double averageConfidence;
    private Double averageCorrectness;
    private Double averageCommunication;
    private Double averageTechnicalAccuracy;
    private Double overallScore;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Builder.Default
    private List<InterviewQuestion> questions = new ArrayList<>();
}