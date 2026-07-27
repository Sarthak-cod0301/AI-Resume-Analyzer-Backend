// dto/RenameResumeRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RenameResumeRequestDTO {

    @NotBlank(message = "Resume name is required")
    private String resumeName;
}