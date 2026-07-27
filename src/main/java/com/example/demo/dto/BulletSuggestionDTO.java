// dto/BulletSuggestionDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulletSuggestionDTO {
    private String type;               // "MISSING_SKILL" or "WEAK_BULLET_REPLACEMENT"
    private String missingSkill;       // populated for MISSING_SKILL type, e.g. "Docker"
    private String originalBullet;     // populated for WEAK_BULLET_REPLACEMENT type, e.g. "Worked on APIs"
    private String suggestedBullet;    // the rewritten/new bullet either way
    private String placementHint;      // which section/project this fits into
    private String rationale;          // why this fits their existing background
}