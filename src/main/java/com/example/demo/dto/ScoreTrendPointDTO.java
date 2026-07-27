// dto/ScoreTrendPointDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoreTrendPointDTO {
    private LocalDateTime date;
    private Integer matchScore;
    private String resumeId;
    private String jobDescriptionId;
}