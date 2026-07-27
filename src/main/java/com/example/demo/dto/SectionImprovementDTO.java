// dto/SectionImprovementDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SectionImprovementDTO {
    private String sectionName;       // "Summary", "Experience", "Projects", "Skills"
    private String originalContent;
    private String improvedContent;
    private String changesSummary;
}