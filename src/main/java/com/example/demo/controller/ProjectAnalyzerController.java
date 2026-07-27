// controller/ProjectAnalyzerController.java
package com.example.demo.controller;

import com.example.demo.dto.ProjectAnalysisResponseDTO;
import com.example.demo.service.ProjectAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-analysis")
@RequiredArgsConstructor
public class ProjectAnalyzerController {

    private final ProjectAnalyzerService projectAnalyzerService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/run")
    public ResponseEntity<ProjectAnalysisResponseDTO> analyze(@RequestParam String resumeId,
                                                                Authentication authentication) {
        ProjectAnalysisResponseDTO result = projectAnalyzerService.analyzeProjects(
                resumeId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ProjectAnalysisResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(projectAnalyzerService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectAnalysisResponseDTO> getById(@PathVariable String id,
                                                               Authentication authentication) {
        return ResponseEntity.ok(projectAnalyzerService.getById(id, currentUserId(authentication)));
    }
}