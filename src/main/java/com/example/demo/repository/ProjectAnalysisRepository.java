// repository/ProjectAnalysisRepository.java
package com.example.demo.repository;

import com.example.demo.entity.ProjectAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectAnalysisRepository extends MongoRepository<ProjectAnalysis, String> {
    List<ProjectAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);
    Optional<ProjectAnalysis> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}