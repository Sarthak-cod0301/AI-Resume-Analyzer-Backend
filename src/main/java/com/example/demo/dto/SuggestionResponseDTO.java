// dto/SuggestionResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestionResponseDTO {
    private String id;
    private String resumeId;
    private String jobDescriptionId;
    private List<BulletSuggestionDTO> suggestions;
    private LocalDateTime generatedAt;
}