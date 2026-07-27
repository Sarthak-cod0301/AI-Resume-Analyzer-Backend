// repository/ResumeRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<Resume, String> {
    List<Resume> findByUserIdAndStatus(String userId, String status);
    Optional<Resume> findByIdAndUserId(String id, String userId);
    long countByUserIdAndStatus(String userId, String status);
    Optional<Resume> findFirstByUserIdAndStatusOrderByUploadDateDesc(String userId, String status);
    void deleteByUserId(String userId);
}