// dto/WeakWordIssueDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeakWordIssueDTO {
    private String weakWord;        // "Worked on"
    private String suggestedWord;   // "Developed"
    private String context;         // the sentence/phrase it appeared in
}