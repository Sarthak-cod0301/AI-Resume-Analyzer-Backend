// dto/FormattingCheckResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormattingCheckResponseDTO {
    private String id;
    private String resumeId;
    private Integer formattingScore;         // final blended score
    private Integer ruleBasedScore;
    private Integer aiAssessedScore;
    private String aiOverallImpression;      // one short paragraph of AI's overall take
    private List<FormattingIssueDTO> issues; // rule-based + AI issues combined
    private List<String> suggestions;
    private Integer distinctFontsUsed;
    private Integer distinctFontSizesUsed;
    private Integer bulletStyleVariantsUsed;
    private LocalDateTime checkedAt;
}