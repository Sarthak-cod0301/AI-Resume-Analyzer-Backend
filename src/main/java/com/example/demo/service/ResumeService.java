// service/ResumeService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ResumeService {
    ResumeResponseDTO uploadResume(MultipartFile file, String userId);
    List<ResumeResponseDTO> getAllResumes(String userId);
    ResumeResponseDTO renameResume(String resumeId, String userId, String newName);
    void deleteResume(String resumeId, String userId);
    byte[] downloadResume(String resumeId, String userId);
    String getResumeFileName(String resumeId, String userId);
    List<ResumeVersionDTO> getVersionHistory(String resumeId, String userId);
    byte[] downloadVersion(String resumeId, Integer versionNumber, String userId);
    byte[] previewResume(String resumeId, String userId);
    ResumeResponseDTO replaceResume(String resumeId, MultipartFile file, String userId);
}