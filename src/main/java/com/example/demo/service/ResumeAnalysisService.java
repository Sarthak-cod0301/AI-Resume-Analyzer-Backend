// service/ResumeAnalysisService.java
package com.example.demo.service;

import com.example.demo.dto.AnalysisResultDTO;
import java.util.List;

public interface ResumeAnalysisService {
    AnalysisResultDTO analyze(String resumeId, String jobDescriptionId, String userId);
    List<AnalysisResultDTO> getHistory(String userId);
    AnalysisResultDTO getById(String id, String userId);
}