// dto/ResumeVersionDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeVersionDTO {
    private Integer versionNumber;
    private String originalFileName;
    private Long fileSize;
    private LocalDateTime createdAt;
}