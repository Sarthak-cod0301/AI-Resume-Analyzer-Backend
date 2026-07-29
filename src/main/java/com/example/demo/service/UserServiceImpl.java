// service/UserServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.exception.AuthException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.entity.Resume;
import com.example.demo.repository.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileDTO getProfile(String userId) {
        User user = getUser(userId);
        return toDTO(user);
    }

    @Override
    public UserProfileDTO updateProfile(String userId, UpdateProfileRequestDTO request) {
        User user = getUser(userId);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        return toDTO(userRepository.save(user));
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequestDTO request) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private UserProfileDTO toDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
 // service/UserServiceImpl.java
 // Add these fields (constructor injection via @RequiredArgsConstructor picks them up automatically)

 private final ResumeRepository resumeRepository;
 private final JobDescriptionRepository jobDescriptionRepository;
 private final ResumeAnalysisRepository resumeAnalysisRepository;
 private final AtsCheckResultRepository atsCheckResultRepository;
 private final ResumeSuggestionRepository resumeSuggestionRepository;
 private final ResumeImprovementRepository resumeImprovementRepository;
 private final ProjectAnalysisRepository projectAnalysisRepository;
 private final GrammarCheckResultRepository grammarCheckResultRepository;
 private final FormattingCheckResultRepository formattingCheckResultRepository;
 private final InterviewSessionRepository interviewSessionRepository;
 private final PasswordResetTokenRepository resetTokenRepository;
 private final GridFSService gridFSService;

 @Override
 @Transactional
 public void deleteAccount(String userId, DeleteAccountRequestDTO request) {
     User user = getUser(userId);

     if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
         throw new AuthException("Incorrect password - account deletion cancelled");
     }

     // Delete all resume file blobs (and their version history files) from GridFS
     // before wiping the Resume records themselves
     List<Resume> resumes = resumeRepository.findByUserIdAndStatus(userId, "ACTIVE");
     for (Resume resume : resumes) {
         resume.getVersions().forEach(v -> gridFSService.deleteFile(v.getFilePath()));
     }

     // Cascade delete across every module's collection
     resumeRepository.deleteByUserId(userId);
     jobDescriptionRepository.deleteByUserId(userId);
     resumeAnalysisRepository.deleteByUserId(userId);
     atsCheckResultRepository.deleteByUserId(userId);
     resumeSuggestionRepository.deleteByUserId(userId);
     resumeImprovementRepository.deleteByUserId(userId);
     projectAnalysisRepository.deleteByUserId(userId);
     grammarCheckResultRepository.deleteByUserId(userId);
     formattingCheckResultRepository.deleteByUserId(userId);
     interviewSessionRepository.deleteByUserId(userId);
     resetTokenRepository.deleteByUserId(userId);

     // Finally, delete the user account itself
     userRepository.delete(user);
 }
}
