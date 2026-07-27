// dto/GrammarCheckResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrammarCheckResponseDTO {
    private String id;
    private String resumeId;
    private Integer overallScore;
    private List<GrammarIssueDTO> grammarIssues;
    private List<WeakWordIssueDTO> weakWordIssues;
    private List<PassiveVoiceIssueDTO> passiveVoiceIssues;
    private Integer totalIssuesFound;
    private LocalDateTime checkedAt;
}