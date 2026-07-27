// dto/EducationMatchDTO.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EducationMatchDTO {
    private String requiredDegree;
    private Boolean found;
}