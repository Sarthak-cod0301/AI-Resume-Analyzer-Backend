// service/MockInterviewService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import java.util.List;

public interface MockInterviewService {
    InterviewSessionDTO startInterview(StartInterviewRequestDTO request, String userId);
    InterviewQuestionDTO getNextQuestion(String sessionId, String userId);
    AnswerEvaluationDTO submitAnswer(String sessionId, String questionId, SubmitAnswerRequestDTO request, String userId);
    InterviewSessionDTO completeInterview(String sessionId, String userId);
    InterviewSessionDTO getSession(String sessionId, String userId);
    List<InterviewSessionDTO> getHistory(String userId);
}