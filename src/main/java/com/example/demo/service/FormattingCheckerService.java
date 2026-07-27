// service/FormattingCheckerService.java
package com.example.demo.service;

import com.example.demo.dto.FormattingCheckResponseDTO;
import java.util.List;

public interface FormattingCheckerService {
    FormattingCheckResponseDTO runCheck(String resumeId, String userId);
    List<FormattingCheckResponseDTO> getHistory(String userId);
    FormattingCheckResponseDTO getById(String id, String userId);
}