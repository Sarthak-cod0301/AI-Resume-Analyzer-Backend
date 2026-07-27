// controller/FormattingCheckerController.java
package com.example.demo.controller;

import com.example.demo.dto.FormattingCheckResponseDTO;
import com.example.demo.service.FormattingCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formatting-check")
@RequiredArgsConstructor
public class FormattingCheckerController {

    private final FormattingCheckerService formattingCheckerService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/run")
    public ResponseEntity<FormattingCheckResponseDTO> runCheck(@RequestParam String resumeId,
                                                                Authentication authentication) {
        FormattingCheckResponseDTO result = formattingCheckerService.runCheck(resumeId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<FormattingCheckResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(formattingCheckerService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormattingCheckResponseDTO> getById(@PathVariable String id,
                                                               Authentication authentication) {
        return ResponseEntity.ok(formattingCheckerService.getById(id, currentUserId(authentication)));
    }
}