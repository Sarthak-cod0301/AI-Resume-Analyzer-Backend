// service/JobDescriptionServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.JobDescriptionRequestDTO;
import com.example.demo.dto.JobDescriptionResponseDTO;
import com.example.demo.entity.JobDescription;
import com.example.demo.exception.JobDescriptionNotFoundException;
import com.example.demo.repository.JobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobDescriptionServiceImpl implements JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;

    @Override
    public JobDescriptionResponseDTO createJobDescription(JobDescriptionRequestDTO request, String userId) {
        JobDescription jd = JobDescription.builder()
                .title(request.getTitle())
                .company(request.getCompany())
                .description(request.getDescription())
                .userId(userId)
                .createdDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toDTO(jobDescriptionRepository.save(jd));
    }

    @Override
    public List<JobDescriptionResponseDTO> getAllJobDescriptions(String userId) {
        return jobDescriptionRepository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public JobDescriptionResponseDTO getJobDescriptionById(String id, String userId) {
        return toDTO(getOwnedJD(id, userId));
    }

    @Override
    public JobDescriptionResponseDTO updateJobDescription(String id, JobDescriptionRequestDTO request, String userId) {
        JobDescription jd = getOwnedJD(id, userId);
        jd.setTitle(request.getTitle());
        jd.setCompany(request.getCompany());
        jd.setDescription(request.getDescription());
        jd.setUpdatedAt(LocalDateTime.now());
        return toDTO(jobDescriptionRepository.save(jd));
    }

    @Override
    public void deleteJobDescription(String id, String userId) {
        JobDescription jd = getOwnedJD(id, userId);
        jobDescriptionRepository.delete(jd);
    }

    @Override
    public List<JobDescriptionResponseDTO> searchJobDescriptions(String keyword, String userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllJobDescriptions(userId);
        }
        return jobDescriptionRepository.searchByKeyword(userId, keyword.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private JobDescription getOwnedJD(String id, String userId) {
        return jobDescriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new JobDescriptionNotFoundException("Job description not found with id: " + id));
    }

    private JobDescriptionResponseDTO toDTO(JobDescription jd) {
        return JobDescriptionResponseDTO.builder()
                .id(jd.getId())
                .title(jd.getTitle())
                .company(jd.getCompany())
                .description(jd.getDescription())
                .createdDate(jd.getCreatedDate())
                .updatedAt(jd.getUpdatedAt())
                .build();
    }
}