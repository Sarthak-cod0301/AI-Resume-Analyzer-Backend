// controller/JobDescriptionController.java
package com.example.demo.controller;

import com.example.demo.dto.JobDescriptionRequestDTO;
import com.example.demo.dto.JobDescriptionResponseDTO;
import com.example.demo.service.JobDescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-descriptions")
@RequiredArgsConstructor
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<JobDescriptionResponseDTO> create(@Valid @RequestBody JobDescriptionRequestDTO request,
                                                              Authentication authentication) {
        JobDescriptionResponseDTO dto = jobDescriptionService.createJobDescription(request, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<JobDescriptionResponseDTO>> getAll(Authentication authentication) {
        return ResponseEntity.ok(jobDescriptionService.getAllJobDescriptions(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescriptionResponseDTO> getById(@PathVariable String id,
                                                               Authentication authentication) {
        return ResponseEntity.ok(jobDescriptionService.getJobDescriptionById(id, currentUserId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDescriptionResponseDTO> update(@PathVariable String id,
                                                              @Valid @RequestBody JobDescriptionRequestDTO request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(jobDescriptionService.updateJobDescription(id, request, currentUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        jobDescriptionService.deleteJobDescription(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobDescriptionResponseDTO>> search(@RequestParam(required = false) String keyword,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(jobDescriptionService.searchJobDescriptions(keyword, currentUserId(authentication)));
    }
}