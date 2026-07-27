// controller/ResumeSuggestionController.java
package com.example.demo.controller;

import com.example.demo.dto.SuggestionResponseDTO;
import com.example.demo.service.ResumeSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class ResumeSuggestionController {

    private final ResumeSuggestionService resumeSuggestionService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/generate")
    public ResponseEntity<SuggestionResponseDTO> generate(@RequestParam String resumeId,
                                                            @RequestParam String jobDescriptionId,
                                                            Authentication authentication) {
        SuggestionResponseDTO result = resumeSuggestionService.generateSuggestions(
                resumeId, jobDescriptionId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<SuggestionResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(resumeSuggestionService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuggestionResponseDTO> getById(@PathVariable String id,
                                                           Authentication authentication) {
        return ResponseEntity.ok(resumeSuggestionService.getById(id, currentUserId(authentication)));
    }
}