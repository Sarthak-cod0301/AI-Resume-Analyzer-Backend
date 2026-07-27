// dto/JobDescriptionResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobDescriptionResponseDTO {
    private String id;
    private String title;
    private String company;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
}