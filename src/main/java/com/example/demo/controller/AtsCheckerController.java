// controller/AtsCheckerController.java
package com.example.demo.controller;

import com.example.demo.dto.AtsCheckResponseDTO;
import com.example.demo.service.AtsCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ats-check")
@RequiredArgsConstructor
public class AtsCheckerController {

    private final AtsCheckerService atsCheckerService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/run")
    public ResponseEntity<AtsCheckResponseDTO> runCheck(@RequestParam String resumeId,
                                                          Authentication authentication) {
        AtsCheckResponseDTO result = atsCheckerService.runCheck(resumeId, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AtsCheckResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(atsCheckerService.getHistory(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtsCheckResponseDTO> getById(@PathVariable String id,
                                                         Authentication authentication) {
        return ResponseEntity.ok(atsCheckerService.getById(id, currentUserId(authentication)));
    }
}