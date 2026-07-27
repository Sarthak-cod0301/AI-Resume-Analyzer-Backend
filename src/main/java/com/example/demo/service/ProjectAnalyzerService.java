// service/ProjectAnalyzerService.java
package com.example.demo.service;

import com.example.demo.dto.ProjectAnalysisResponseDTO;
import java.util.List;

public interface ProjectAnalyzerService {
    ProjectAnalysisResponseDTO analyzeProjects(String resumeId, String userId);
    List<ProjectAnalysisResponseDTO> getHistory(String userId);
    ProjectAnalysisResponseDTO getById(String id, String userId);
}