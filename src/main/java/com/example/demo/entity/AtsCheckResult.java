// entity/AtsCheckResult.java
package com.example.demo.entity;

import com.example.demo.dto.AtsIssueDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "ats_check_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtsCheckResult {

    @Id
    private String id;

    private String resumeId;
    private String userId;

    private Integer atsScore;
    private List<String> suggestions;
    private List<AtsIssueDTO> issues;   // embedded sub-documents, no JSON-string workaround needed

    private Integer wordCount;
    private Integer pageCount;
    private Long fileSizeBytes;
    private Double keywordDensityPercent;

    private LocalDateTime checkedAt;
}