// dto/FormattingIssueDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormattingIssueDTO {
    private String category;     // "Alignment", "Font Consistency", "Bullet Points", "Spacing", "Headings"
    private String severity;     // "HIGH", "MEDIUM", "LOW"
    private String message;
    private String suggestion;
    private String source;       // "RULE_BASED" or "AI"
}