// dto/AtsImprovementPointDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtsImprovementPointDTO {
    private LocalDateTime date;
    private Integer atsScore;
    private String resumeId;
}