// service/ResumeSuggestionService.java
package com.example.demo.service;

import com.example.demo.dto.SuggestionResponseDTO;
import java.util.List;

public interface ResumeSuggestionService {
    SuggestionResponseDTO generateSuggestions(String resumeId, String jobDescriptionId, String userId);
    List<SuggestionResponseDTO> getHistory(String userId);
    SuggestionResponseDTO getById(String id, String userId);
}