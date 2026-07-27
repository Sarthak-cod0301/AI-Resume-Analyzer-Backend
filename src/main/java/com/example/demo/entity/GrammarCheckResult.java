// entity/GrammarCheckResult.java
package com.example.demo.entity;

import com.example.demo.dto.GrammarIssueDTO;
import com.example.demo.dto.PassiveVoiceIssueDTO;
import com.example.demo.dto.WeakWordIssueDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "grammar_check_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrammarCheckResult {

    @Id
    private String id;

    private String resumeId;
    private String userId;

    private Integer overallScore;
    private List<GrammarIssueDTO> grammarIssues;
    private List<WeakWordIssueDTO> weakWordIssues;
    private List<PassiveVoiceIssueDTO> passiveVoiceIssues;
    private Integer totalIssuesFound;

    private LocalDateTime checkedAt;
}