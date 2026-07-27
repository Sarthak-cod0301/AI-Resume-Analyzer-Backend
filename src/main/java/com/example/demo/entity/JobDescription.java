// entity/JobDescription.java
package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "job_descriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobDescription {

    @Id
    private String id;

    private String userId;

    private String title;
    private String company;
    private String description;   // plain String — no @Lob/columnDefinition needed in Mongo

    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
}