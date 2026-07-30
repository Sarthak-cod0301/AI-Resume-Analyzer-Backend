// service/MockInterviewServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.exception.InterviewException;
import com.example.demo.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MockInterviewServiceImpl implements MockInterviewService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_QUESTION_COUNT = 5;

    @Override
    public InterviewSessionDTO startInterview(StartInterviewRequestDTO request, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), userId)
                .orElseThrow(() -> new InterviewException("Resume not found or not owned by user"));

        JobDescription jd = null;
        if (request.getJobDescriptionId() != null && !request.getJobDescriptionId().isBlank()) {
            jd = jobDescriptionRepository.findByIdAndUserId(request.getJobDescriptionId(), userId)
                    .orElseThrow(() -> new InterviewException("Job description not found or not owned by user"));
        }

        String resumeText =
textExtractionService.extractText(
        resume.getGridFsId(),
        resume.getFileType());
        int questionCount = request.getNumberOfQuestions() != null ? request.getNumberOfQuestions() : DEFAULT_QUESTION_COUNT;

        String prompt = buildQuestionGenerationPrompt(resumeText, jd != null ? jd.getDescription() : null, questionCount);
        String geminiResponse = geminiService.generateContent(prompt);
        List<GeneratedQuestion> generated = parseGeneratedQuestions(geminiResponse);

        InterviewSession session = InterviewSession.builder()
                .resumeId(request.getResumeId())
                .jobDescriptionId(request.getJobDescriptionId())
                .userId(userId)
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .build();

        int order = 1;
        for (GeneratedQuestion gq : generated) {
            InterviewQuestion question = InterviewQuestion.builder()
                    .questionId(UUID.randomUUID().toString())
                    .questionOrder(order++)
                    .questionText(gq.questionText)
                    .questionType(gq.questionType)
                    .answer(null)
                    .build();
            session.getQuestions().add(question);
        }

        session = sessionRepository.save(session);
        return toDTO(session);
    }

    private String buildQuestionGenerationPrompt(String resumeText, String jdText, int count) {
        String jdSection = jdText != null
                ? "JOB DESCRIPTION:\n" + jdText
                : "No specific job description provided - generate general Full Stack Developer interview questions appropriate for this candidate's experience level.";

        return """
            You are a senior technical interviewer conducting a mock interview. Based on the
            candidate's RESUME below (and the job description if provided), generate exactly %d
            interview questions covering a mix of:
            - TECHNICAL: questions about specific technologies/concepts mentioned in the resume
            - PROJECT_SPECIFIC: questions that dig into a specific project the candidate listed
            - BEHAVIORAL: standard behavioral questions relevant to their experience level

            Return ONLY a valid JSON array (no markdown, no extra text) with EXACTLY this structure:

            [
              { "questionText": "<the interview question>", "questionType": "TECHNICAL | PROJECT_SPECIFIC | BEHAVIORAL" }
            ]

            Rules:
            - Base TECHNICAL and PROJECT_SPECIFIC questions only on skills/projects actually mentioned in the resume.
            - Keep questions realistic, like something an actual interviewer would ask a fresher/entry-level candidate.
            - Vary difficulty: mostly foundational with 1-2 slightly deeper questions.
            - Return ONLY the JSON array, nothing else.

            RESUME:
            %s

            %s
            """.formatted(count, resumeText, jdSection);
    }

    private List<GeneratedQuestion> parseGeneratedQuestions(String jsonResponse) {
        try {
            String cleaned = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<List<GeneratedQuestion>>() {});
        } catch (Exception e) {
            throw new InterviewException("Failed to parse AI-generated interview questions", e);
        }
    }

    @Override
    public InterviewQuestionDTO getNextQuestion(String sessionId, String userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);

        Optional<InterviewQuestion> nextUnanswered = session.getQuestions().stream()
                .filter(q -> q.getAnswer() == null)
                .findFirst();

        if (nextUnanswered.isEmpty()) {
            throw new InterviewException("All questions in this session have been answered");
        }

        InterviewQuestion question = nextUnanswered.get();
        return InterviewQuestionDTO.builder()
                .questionId(question.getQuestionId())
                .questionOrder(question.getQuestionOrder())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .answered(false)
                .build();
    }

    @Override
    public AnswerEvaluationDTO submitAnswer(String sessionId, String questionId, SubmitAnswerRequestDTO request, String userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);

        InterviewQuestion question = session.getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new InterviewException("Question not found in this session"));

        if (question.getAnswer() != null) {
            throw new InterviewException("This question has already been answered");
        }

        String prompt = buildEvaluationPrompt(question.getQuestionText(), request.getAnswerText(), question.getQuestionType());
        String geminiResponse = geminiService.generateContent(prompt);
        EvaluationRaw eval = parseEvaluation(geminiResponse);

        InterviewAnswer answer = InterviewAnswer.builder()
                .answerText(request.getAnswerText())
                .confidenceScore(clamp(eval.confidenceScore))
                .correctnessScore(clamp(eval.correctnessScore))
                .communicationScore(clamp(eval.communicationScore))
                .technicalAccuracyScore(clamp(eval.technicalAccuracyScore))
                .feedback(eval.feedback)
                .idealAnswerHint(eval.idealAnswerHint)
                .answeredAt(LocalDateTime.now())
                .build();

        question.setAnswer(answer);
        sessionRepository.save(session); // saves the whole document, embedded answer included

        return AnswerEvaluationDTO.builder()
                .questionId(question.getQuestionId())
                .questionText(question.getQuestionText())
                .answerText(answer.getAnswerText())
                .confidenceScore(answer.getConfidenceScore())
                .correctnessScore(answer.getCorrectnessScore())
                .communicationScore(answer.getCommunicationScore())
                .technicalAccuracyScore(answer.getTechnicalAccuracyScore())
                .feedback(answer.getFeedback())
                .idealAnswerHint(answer.getIdealAnswerHint())
                .build();
    }

    private String buildEvaluationPrompt(String question, String answer, String questionType) {
        return """
            You are an expert technical interviewer evaluating a candidate's typed answer during a
            mock interview.

            QUESTION (%s): %s

            CANDIDATE'S ANSWER: %s

            Evaluate the answer on these 4 dimensions, each scored 0-100:
            - confidenceScore: how confidently/assertively the answer is phrased (tone and certainty of language, not correctness)
            - correctnessScore: factual/technical correctness of the content
            - communicationScore: clarity, structure, and how well the answer is articulated
            - technicalAccuracyScore: depth and precision of technical details given (for BEHAVIORAL questions, score based on relevance and specificity instead)

            Return ONLY a valid JSON object (no markdown, no extra text) with EXACTLY this structure:

            {
              "confidenceScore": <0-100>,
              "correctnessScore": <0-100>,
              "communicationScore": <0-100>,
              "technicalAccuracyScore": <0-100>,
              "feedback": "<2-3 sentences of constructive feedback on this specific answer>",
              "idealAnswerHint": "<1-2 sentences describing what a strong answer would have included>"
            }

            Rules:
            - Be honest and constructive, not just encouraging - this is for genuine interview prep.
            - If the answer is empty, off-topic, or says "I don't know", score correctness and technicalAccuracy very low but still give constructive feedback.
            - Return ONLY the JSON object, nothing else.
            """.formatted(questionType, question, answer);
    }

    private EvaluationRaw parseEvaluation(String jsonResponse) {
        try {
            String cleaned = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, EvaluationRaw.class);
        } catch (Exception e) {
            throw new InterviewException("Failed to parse AI evaluation response", e);
        }
    }

    private int clamp(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }

    @Override
    public InterviewSessionDTO completeInterview(String sessionId, String userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);

        List<InterviewAnswer> answers = session.getQuestions().stream()
                .map(InterviewQuestion::getAnswer)
                .filter(a -> a != null)
                .collect(Collectors.toList());

        if (answers.isEmpty()) {
            throw new InterviewException("Cannot complete an interview with no answered questions");
        }

        double avgConfidence = answers.stream().mapToInt(InterviewAnswer::getConfidenceScore).average().orElse(0);
        double avgCorrectness = answers.stream().mapToInt(InterviewAnswer::getCorrectnessScore).average().orElse(0);
        double avgCommunication = answers.stream().mapToInt(InterviewAnswer::getCommunicationScore).average().orElse(0);
        double avgTechnical = answers.stream().mapToInt(InterviewAnswer::getTechnicalAccuracyScore).average().orElse(0);
        double overall = (avgConfidence + avgCorrectness + avgCommunication + avgTechnical) / 4.0;

        session.setAverageConfidence(round1(avgConfidence));
        session.setAverageCorrectness(round1(avgCorrectness));
        session.setAverageCommunication(round1(avgCommunication));
        session.setAverageTechnicalAccuracy(round1(avgTechnical));
        session.setOverallScore(round1(overall));
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());

        session = sessionRepository.save(session);
        return toDTO(session);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Override
    public InterviewSessionDTO getSession(String sessionId, String userId) {
        return toDTO(getOwnedSession(sessionId, userId));
    }

    @Override
    public List<InterviewSessionDTO> getHistory(String userId) {
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteSession(String sessionId, String userId) {
        // Verify ownership first so a user can't delete another user's session
        // just by guessing/passing a different sessionId.
        getOwnedSession(sessionId, userId);
        sessionRepository.deleteByIdAndUserId(sessionId, userId);
    }

    private InterviewSession getOwnedSession(String sessionId, String userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new InterviewException("Interview session not found"));
    }

    private InterviewSessionDTO toDTO(InterviewSession session) {
        List<InterviewQuestionDTO> questionDTOs = session.getQuestions().stream()
                .map(q -> InterviewQuestionDTO.builder()
                        .questionId(q.getQuestionId())
                        .questionOrder(q.getQuestionOrder())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .answered(q.getAnswer() != null)
                        .build())
                .collect(Collectors.toList());

        List<AnswerEvaluationDTO> answerDTOs = session.getQuestions().stream()
                .filter(q -> q.getAnswer() != null)
                .map(q -> {
                    InterviewAnswer a = q.getAnswer();
                    return AnswerEvaluationDTO.builder()
                            .questionId(q.getQuestionId())
                            .questionText(q.getQuestionText())
                            .answerText(a.getAnswerText())
                            .confidenceScore(a.getConfidenceScore())
                            .correctnessScore(a.getCorrectnessScore())
                            .communicationScore(a.getCommunicationScore())
                            .technicalAccuracyScore(a.getTechnicalAccuracyScore())
                            .feedback(a.getFeedback())
                            .idealAnswerHint(a.getIdealAnswerHint())
                            .build();
                })
                .collect(Collectors.toList());

        return InterviewSessionDTO.builder()
                .id(session.getId())
                .resumeId(session.getResumeId())
                .jobDescriptionId(session.getJobDescriptionId())
                .status(session.getStatus())
                .questions(questionDTOs)
                .answers(answerDTOs)
                .averageConfidence(session.getAverageConfidence())
                .averageCorrectness(session.getAverageCorrectness())
                .averageCommunication(session.getAverageCommunication())
                .averageTechnicalAccuracy(session.getAverageTechnicalAccuracy())
                .overallScore(session.getOverallScore())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    // Internal helpers matching Gemini's raw JSON shapes
    private static class GeneratedQuestion {
        public String questionText;
        public String questionType;
    }

    private static class EvaluationRaw {
        public Integer confidenceScore;
        public Integer correctnessScore;
        public Integer communicationScore;
        public Integer technicalAccuracyScore;
        public String feedback;
        public String idealAnswerHint;
    }
}
