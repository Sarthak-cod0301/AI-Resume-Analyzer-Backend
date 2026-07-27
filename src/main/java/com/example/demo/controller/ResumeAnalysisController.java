// controller/ResumeAnalysisController.java
package com.example.demo.controller;

import com.example.demo.dto.AnalysisResultDTO;
import com.example.demo.service.ResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class ResumeAnalysisController {

    private final ResumeAnalysisService resumeAnalysisService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/run")
    public ResponseEntity<AnalysisResultDTO> analyze(@RequestParam String resumeId,
                                                       @RequestParam String jobDescriptionId,
                                                       Authentication authentication) {
        AnalysisResultDTO result = resumeAnalysisService.analyze(
                resumeId, jobDescriptionId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResultDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(resumeAnalysisService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResultDTO> getById(@PathVariable String id,
                                                       Authentication authentication) {
        return ResponseEntity.ok(resumeAnalysisService.getById(id, currentUserId(authentication)));
    }
}