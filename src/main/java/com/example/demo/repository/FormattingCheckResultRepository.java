// repository/FormattingCheckResultRepository.java
package com.example.demo.repository;

import com.example.demo.entity.FormattingCheckResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FormattingCheckResultRepository extends MongoRepository<FormattingCheckResult, String> {
    List<FormattingCheckResult> findByUserIdOrderByCheckedAtDesc(String userId);
    void deleteByUserId(String userId);
    Optional<FormattingCheckResult> findByIdAndUserId(String id, String userId);
}