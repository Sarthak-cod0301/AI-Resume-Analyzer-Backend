// dto/ExperienceMatchDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExperienceMatchDTO {
    private Double requiredYears;
    private Double foundYears;
    private Boolean meetsRequirement;
}