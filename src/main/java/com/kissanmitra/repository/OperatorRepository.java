package com.kissanmitra.repository;

import com.kissanmitra.entity.Operator;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Operator entity.
 */
@Repository
public interface OperatorRepository extends MongoRepository<Operator, String> {

    /**
     * Finds operator by user ID.
     *
     * @param userId user ID
     * @return Optional Operator
     */
    Optional<Operator> findByUserId(String userId);

    /**
     * Finds all active operators.
     *
     * @param status status value
     * @return list of operators
     */
    List<Operator> findByStatus(String status);
}

