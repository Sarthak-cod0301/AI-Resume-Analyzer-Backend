// service/JobDescriptionService.java
package com.example.demo.service;

import com.example.demo.dto.JobDescriptionRequestDTO;
import com.example.demo.dto.JobDescriptionResponseDTO;
import java.util.List;

public interface JobDescriptionService {
    JobDescriptionResponseDTO createJobDescription(JobDescriptionRequestDTO request, String userId);
    List<JobDescriptionResponseDTO> getAllJobDescriptions(String userId);
    JobDescriptionResponseDTO getJobDescriptionById(String id, String userId);
    JobDescriptionResponseDTO updateJobDescription(String id, JobDescriptionRequestDTO request, String userId);
    void deleteJobDescription(String id, String userId);
    List<JobDescriptionResponseDTO> searchJobDescriptions(String keyword, String userId);
}