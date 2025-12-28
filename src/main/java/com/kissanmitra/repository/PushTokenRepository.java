package com.kissanmitra.repository;

import com.kissanmitra.entity.PushToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PushToken entity.
 */
@Repository
public interface PushTokenRepository extends MongoRepository<PushToken, String> {

    /**
     * Finds push token by user ID and platform.
     *
     * @param userId user ID
     * @param platform platform
     * @return Optional PushToken
     */
    Optional<PushToken> findByUserIdAndPlatform(String userId, String platform);

    /**
     * Finds all active push tokens for a user.
     *
     * @param userId user ID
     * @return list of push tokens
     */
    List<PushToken> findByUserIdAndActiveTrue(String userId);
}

