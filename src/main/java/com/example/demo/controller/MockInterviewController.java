// controller/MockInterviewController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.MockInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewSessionDTO> start(@Valid @RequestBody StartInterviewRequestDTO request,
                                                       Authentication authentication) {
        InterviewSessionDTO session = mockInterviewService.startInterview(request, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{sessionId}/next-question")
    public ResponseEntity<InterviewQuestionDTO> nextQuestion(@PathVariable String sessionId,
                                                               Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getNextQuestion(sessionId, currentUserId(authentication)));
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    public ResponseEntity<AnswerEvaluationDTO> submitAnswer(@PathVariable String sessionId,
                                                              @PathVariable String questionId,
                                                              @Valid @RequestBody SubmitAnswerRequestDTO request,
                                                              Authentication authentication) {
        AnswerEvaluationDTO result = mockInterviewService.submitAnswer(
                sessionId, questionId, request, currentUserId(authentication));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<InterviewSessionDTO> complete(@PathVariable String sessionId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.completeInterview(sessionId, currentUserId(authentication)));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionDTO> getSession(@PathVariable String sessionId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getSession(sessionId, currentUserId(authentication)));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InterviewSessionDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getHistory(currentUserId(authentication)));
    }
}