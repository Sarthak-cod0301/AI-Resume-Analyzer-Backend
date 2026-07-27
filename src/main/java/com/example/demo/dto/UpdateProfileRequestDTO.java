// dto/UpdateProfileRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;
}