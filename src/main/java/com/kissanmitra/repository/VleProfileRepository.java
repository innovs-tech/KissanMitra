package com.kissanmitra.repository;

import com.kissanmitra.entity.VleProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for VleProfile entity.
 */
@Repository
public interface VleProfileRepository extends MongoRepository<VleProfile, String> {

    /**
     * Finds VLE profile by user ID.
     *
     * @param userId user ID
     * @return Optional VleProfile
     */
    Optional<VleProfile> findByUserId(String userId);
}

