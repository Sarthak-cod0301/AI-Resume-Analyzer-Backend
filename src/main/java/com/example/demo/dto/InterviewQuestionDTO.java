// dto/InterviewQuestionDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewQuestionDTO {
    private String questionId;
    private Integer questionOrder;
    private String questionText;
    private String questionType;
    private Boolean answered;
}