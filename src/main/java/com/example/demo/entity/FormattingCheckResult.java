// entity/FormattingCheckResult.java
package com.example.demo.entity;

import com.example.demo.dto.FormattingIssueDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "formatting_check_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormattingCheckResult {

    @Id
    private String id;

    private String resumeId;
    private String userId;

    private Integer formattingScore;
    private Integer ruleBasedScore;
    private Integer aiAssessedScore;
    private String aiOverallImpression;

    private List<FormattingIssueDTO> issues;   // embedded, rule-based + AI combined
    private List<String> suggestions;

    private Integer distinctFontsUsed;
    private Integer distinctFontSizesUsed;
    private Integer bulletStyleVariantsUsed;

    private LocalDateTime checkedAt;
}