// entity/User.java
package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    private String id;

    private String name;
    private String email;
    private String password;   // BCrypt hashed
    private String phone;      // optional profile field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean active = true;
}