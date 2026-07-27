// dto/StartInterviewRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StartInterviewRequestDTO {

    @NotBlank(message = "Resume ID is required")
    private String resumeId;

    private String jobDescriptionId; // optional
    private Integer numberOfQuestions; // optional, defaults to 5
}