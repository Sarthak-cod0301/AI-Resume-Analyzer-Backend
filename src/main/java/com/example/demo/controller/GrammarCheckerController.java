// controller/GrammarCheckerController.java
package com.example.demo.controller;

import com.example.demo.dto.GrammarCheckResponseDTO;
import com.example.demo.service.GrammarCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grammar-check")
@RequiredArgsConstructor
public class GrammarCheckerController {

    private final GrammarCheckerService grammarCheckerService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/run")
    public ResponseEntity<GrammarCheckResponseDTO> runCheck(@RequestParam String resumeId,
                                                              Authentication authentication) {
        GrammarCheckResponseDTO result = grammarCheckerService.runCheck(resumeId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<GrammarCheckResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(grammarCheckerService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrammarCheckResponseDTO> getById(@PathVariable String id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(grammarCheckerService.getById(id, currentUserId(authentication)));
    }
}