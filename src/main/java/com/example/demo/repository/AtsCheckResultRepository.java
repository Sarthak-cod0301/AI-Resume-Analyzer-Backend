// repository/AtsCheckResultRepository.java
package com.example.demo.repository;

import com.example.demo.entity.AtsCheckResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AtsCheckResultRepository extends MongoRepository<AtsCheckResult, String> {
    List<AtsCheckResult> findByUserIdOrderByCheckedAtDesc(String userId);
    List<AtsCheckResult> findByUserIdOrderByCheckedAtAsc(String userId);
    Optional<AtsCheckResult> findByIdAndUserId(String id, String userId);

    @Query(value = "{ 'userId': ?0 }", fields = "{ 'atsScore': 1 }")
    List<AtsCheckResult> findScoresByUserId(String userId);
    void deleteByUserId(String userId);
}