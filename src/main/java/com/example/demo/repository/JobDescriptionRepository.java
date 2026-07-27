// repository/JobDescriptionRepository.java
package com.example.demo.repository;

import com.example.demo.entity.JobDescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface JobDescriptionRepository extends MongoRepository<JobDescription, String> {

    List<JobDescription> findByUserIdOrderByCreatedDateDesc(String userId);

    Optional<JobDescription> findByIdAndUserId(String id, String userId);

    Optional<JobDescription> findFirstByUserIdOrderByCreatedDateDesc(String userId);

    // Case-insensitive search across title, company, and description
    @Query("{ '$and': [ { 'userId': ?0 }, { '$or': [ " +
           "{ 'title': { $regex: ?1, $options: 'i' } }, " +
           "{ 'company': { $regex: ?1, $options: 'i' } }, " +
           "{ 'description': { $regex: ?1, $options: 'i' } } " +
           "] } ] }")
    List<JobDescription> searchByKeyword(String userId, String keyword);
    void deleteByUserId(String userId);
}