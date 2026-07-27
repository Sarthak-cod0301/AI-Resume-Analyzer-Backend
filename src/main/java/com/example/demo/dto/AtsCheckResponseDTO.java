// dto/AtsCheckResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtsCheckResponseDTO {
    private String id;
    private String resumeId;
    private Integer atsScore;
    private List<String> suggestions;
    private List<AtsIssueDTO> issues;
    private Integer wordCount;
    private Integer pageCount;
    private Long fileSizeBytes;
    private Double keywordDensityPercent;
    private LocalDateTime checkedAt;
}