// repository/ResumeAnalysisRepository.java
package com.example.demo.repository;

import com.example.demo.entity.ResumeAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ResumeAnalysisRepository extends MongoRepository<ResumeAnalysis, String> {

    long countByUserId(String userId);

    List<ResumeAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);

    List<ResumeAnalysis> findByUserIdOrderByAnalyzedAtAsc(String userId);

    Optional<ResumeAnalysis> findByIdAndUserId(String id, String userId);

    List<ResumeAnalysis> findByResumeIdAndUserId(String resumeId, String userId);

    void deleteByUserId(String userId);
    
    @Query(value = "{ 'userId': ?0 }", sort = "{ 'matchScore': -1 }")
    List<ResumeAnalysis> findTopByUserIdOrderByMatchScoreDesc(String userId);
}