// entity/InterviewAnswer.java — embedded inside InterviewQuestion
package com.example.demo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewAnswer {
    private String answerText;

    private Integer confidenceScore;
    private Integer correctnessScore;
    private Integer communicationScore;
    private Integer technicalAccuracyScore;

    private String feedback;
    private String idealAnswerHint;

    private LocalDateTime answeredAt;
}