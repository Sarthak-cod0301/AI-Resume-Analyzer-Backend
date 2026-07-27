// dto/GrammarIssueDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrammarIssueDTO {
    private String originalText;
    private String correctedText;
    private String explanation;
}