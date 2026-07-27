// dto/ImprovementResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImprovementResponseDTO {
    private String id;
    private String resumeId;
    private List<SectionImprovementDTO> sections;
    private LocalDateTime generatedAt;
}