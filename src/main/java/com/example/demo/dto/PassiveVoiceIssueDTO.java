// dto/PassiveVoiceIssueDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PassiveVoiceIssueDTO {
    private String originalSentence;
    private String suggestedActiveSentence;
}