// entity/PasswordResetToken.java
package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    private String id;

    private String userId;
    private String token;
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean used = false;
}