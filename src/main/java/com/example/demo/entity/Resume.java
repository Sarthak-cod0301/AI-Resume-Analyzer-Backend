// entity/Resume.java
package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "resumes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {

    @Id
    private String id;

    private String userId;

    private String resumeName;      // display/rename-able name
    private String storedFileName;  // current file on disk (UUID based)
    private String gridFsId;      // current full path on disk
    private String fileType;        // pdf / docx
    private Long fileSize;

    private String status;          // "ACTIVE" or "DELETED" (soft-delete flag, matches your spec's "status" field)

    private LocalDateTime uploadDate;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ResumeVersion> versions = new ArrayList<>();
}
