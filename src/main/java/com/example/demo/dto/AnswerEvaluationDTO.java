// dto/AnswerEvaluationDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnswerEvaluationDTO {
    private String questionId;
    private String questionText;
    private String answerText;
    private Integer confidenceScore;
    private Integer correctnessScore;
    private Integer communicationScore;
    private Integer technicalAccuracyScore;
    private String feedback;
    private String idealAnswerHint;
}