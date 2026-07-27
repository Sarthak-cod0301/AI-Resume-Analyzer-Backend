// controller/ResumeImprovementController.java
package com.example.demo.controller;

import com.example.demo.dto.ImprovementResponseDTO;
import com.example.demo.service.ResumeImprovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/improvement")
@RequiredArgsConstructor
public class ResumeImprovementController {

    private final ResumeImprovementService resumeImprovementService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/improve")
    public ResponseEntity<ImprovementResponseDTO> improve(@RequestParam String resumeId,
                                                            Authentication authentication) {
        ImprovementResponseDTO result = resumeImprovementService.improveResume(
                resumeId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ImprovementResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(resumeImprovementService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImprovementResponseDTO> getById(@PathVariable String id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(resumeImprovementService.getById(id, currentUserId(authentication)));
    }
}