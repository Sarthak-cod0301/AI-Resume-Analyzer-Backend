// dto/DeleteAccountRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeleteAccountRequestDTO {

    @NotBlank(message = "Password confirmation is required")
    private String password;   // require re-entering password before deletion, as a safety check
}