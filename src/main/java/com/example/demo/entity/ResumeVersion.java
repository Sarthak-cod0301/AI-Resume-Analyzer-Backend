// entity/ResumeVersion.java — embedded inside Resume, not its own collection
package com.example.demo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeVersion {
    private Integer versionNumber;
    private String storedFileName;
    private String filePath;
    private String originalFileName;
    private Long fileSize;
    private LocalDateTime createdAt;
}