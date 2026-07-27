// dto/UserProfileDTO.java
package com.example.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
}