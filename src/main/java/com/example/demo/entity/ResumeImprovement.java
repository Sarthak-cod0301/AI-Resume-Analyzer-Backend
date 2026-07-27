// entity/ResumeImprovement.java
package com.example.demo.entity;

import com.example.demo.dto.SectionImprovementDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resume_improvements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeImprovement {

    @Id
    private String id;

    private String resumeId;
    private String userId;

    private List<SectionImprovementDTO> sections; // embedded sub-documents

    private LocalDateTime generatedAt;
}