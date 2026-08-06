
//dto/StartInterviewRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StartInterviewRequestDTO {

 @NotBlank(message = "Resume ID is required")
 private String resumeId;

 private String jobDescriptionId; // optional

 // Deprecated: interview length is now fixed at 10 questions server-side.
 // Kept only for backward compatibility with older clients; the value is ignored.
 private Integer numberOfQuestions;
}
