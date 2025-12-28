package com.kissanmitra.repository;

import com.kissanmitra.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Finds notifications by user ID.
     *
     * @param userId user ID
     * @return list of notifications
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}

