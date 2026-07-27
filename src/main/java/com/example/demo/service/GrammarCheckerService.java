// service/GrammarCheckerService.java
package com.example.demo.service;

import com.example.demo.dto.GrammarCheckResponseDTO;
import java.util.List;

public interface GrammarCheckerService {
    GrammarCheckResponseDTO runCheck(String resumeId, String userId);
    List<GrammarCheckResponseDTO> getHistory(String userId);
    GrammarCheckResponseDTO getById(String id, String userId);
}