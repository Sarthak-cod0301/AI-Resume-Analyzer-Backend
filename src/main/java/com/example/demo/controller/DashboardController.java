// controller/DashboardController.java
package com.example.demo.controller;

import com.example.demo.dto.DashboardSummaryDTO;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<DashboardSummaryDTO> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getDashboard(currentUserId(authentication)));
    }
}