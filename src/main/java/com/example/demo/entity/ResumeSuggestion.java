// entity/ResumeSuggestion.java
package com.example.demo.entity;

import com.example.demo.dto.BulletSuggestionDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resume_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeSuggestion {

    @Id
    private String id;

    private String resumeId;
    private String jobDescriptionId;
    private String userId;

    private List<BulletSuggestionDTO> suggestions; // embedded, no JSON-string workaround

    private LocalDateTime generatedAt;
}