// dto/AtsIssueDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtsIssueDTO {
    private String category;      // "Font", "Tables", "Images", "Icons", "Headers", "Footer", "Keyword Density", "File Size", "Resume Length", "Headings"
    private String severity;      // "HIGH", "MEDIUM", "LOW"
    private String message;
    private String suggestion;
}