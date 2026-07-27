// service/AtsCheckerService.java
package com.example.demo.service;

import com.example.demo.dto.AtsCheckResponseDTO;
import java.util.List;

public interface AtsCheckerService {
    AtsCheckResponseDTO runCheck(String resumeId, String userId);
    List<AtsCheckResponseDTO> getHistory(String userId);
    AtsCheckResponseDTO getById(String id, String userId);
}