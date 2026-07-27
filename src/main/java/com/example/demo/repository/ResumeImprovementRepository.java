// repository/ResumeImprovementRepository.java
package com.example.demo.repository;

import com.example.demo.entity.ResumeImprovement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeImprovementRepository extends MongoRepository<ResumeImprovement, String> {
    List<ResumeImprovement> findByUserIdOrderByGeneratedAtDesc(String userId);
    void deleteByUserId(String userId);
    Optional<ResumeImprovement> findByIdAndUserId(String id, String userId);
}