package com.kissanmitra.repository;

import com.kissanmitra.entity.DiscoveryIntent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DiscoveryIntent entity.
 */
@Repository
public interface DiscoveryIntentRepository extends MongoRepository<DiscoveryIntent, String> {

    /**
     * Finds intent by ID and status.
     *
     * @param id intent ID
     * @param status status
     * @return Optional DiscoveryIntent
     */
    Optional<DiscoveryIntent> findByIdAndStatus(String id, String status);
}

