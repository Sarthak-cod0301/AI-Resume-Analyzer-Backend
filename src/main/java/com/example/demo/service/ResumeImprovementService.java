// service/ResumeImprovementService.java
package com.example.demo.service;

import com.example.demo.dto.ImprovementResponseDTO;
import java.util.List;

public interface ResumeImprovementService {
    ImprovementResponseDTO improveResume(String resumeId, String userId);
    List<ImprovementResponseDTO> getHistory(String userId);
    ImprovementResponseDTO getById(String id, String userId);
}