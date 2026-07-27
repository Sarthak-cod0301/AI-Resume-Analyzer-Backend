// dto/KeywordAnalysisDTO.java
package com.example.demo.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KeywordAnalysisDTO {
    private List<String> keywordsPresent;
    private List<String> keywordsMissing;
}