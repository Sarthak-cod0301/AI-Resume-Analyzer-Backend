// entity/InterviewQuestion.java — embedded inside InterviewSession
package com.example.demo.entity;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewQuestion {
    private String questionId;      // manually assigned UUID string, since this isn't its own @Document
    private Integer questionOrder;
    private String questionText;
    private String questionType;    // "TECHNICAL", "BEHAVIORAL", "PROJECT_SPECIFIC"

    private InterviewAnswer answer; // null until answered
}