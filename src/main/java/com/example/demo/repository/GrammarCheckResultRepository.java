// repository/GrammarCheckResultRepository.java
package com.example.demo.repository;

import com.example.demo.entity.GrammarCheckResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface GrammarCheckResultRepository extends MongoRepository<GrammarCheckResult, String> {
    List<GrammarCheckResult> findByUserIdOrderByCheckedAtDesc(String userId);
    Optional<GrammarCheckResult> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}