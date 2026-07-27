// dto/SkillGapItemDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillGapItemDTO {
    private String skillName;
    private Integer timesMissing;
}