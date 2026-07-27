// dto/SubmitAnswerRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmitAnswerRequestDTO {

    @NotBlank(message = "Answer cannot be empty")
    private String answerText;
}