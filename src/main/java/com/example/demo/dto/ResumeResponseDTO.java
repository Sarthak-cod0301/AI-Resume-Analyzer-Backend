// dto/ResumeResponseDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeResponseDTO {
    private String id;
    private String resumeName;
    private String fileType;
    private Long fileSize;
    private String status;
    private LocalDateTime uploadDate;
    private LocalDateTime updatedAt;
    private Integer totalVersions;
}