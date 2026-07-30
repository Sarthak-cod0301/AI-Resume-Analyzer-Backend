// repository/InterviewSessionRepository.java
package com.example.demo.repository;

import com.example.demo.entity.InterviewSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends MongoRepository<InterviewSession, String> {
    List<InterviewSession> findByUserIdOrderByStartedAtDesc(String userId);
    void deleteByUserId(String userId);
    Optional<InterviewSession> findByIdAndUserId(String id, String userId);
    void deleteByIdAndUserId(String id, String userId);
}
