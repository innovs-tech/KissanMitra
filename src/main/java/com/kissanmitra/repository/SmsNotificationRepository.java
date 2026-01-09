package com.kissanmitra.repository;

import com.kissanmitra.entity.SmsNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SmsNotification entity.
 */
@Repository
public interface SmsNotificationRepository extends MongoRepository<SmsNotification, String> {

    /**
     * Finds all SMS notifications for a specific user.
     *
     * @param userId user ID
     * @return list of SMS notifications
     */
    List<SmsNotification> findByUserId(String userId);

    /**
     * Finds all SMS notifications for a specific phone number.
     *
     * @param phoneNumber phone number (E.164 format)
     * @return list of SMS notifications
     */
    List<SmsNotification> findByTo(String phoneNumber);

    /**
     * Finds all SMS notifications for a related entity.
     *
     * @param relatedEntityType entity type (ORDER, LEASE)
     * @param relatedEntityId entity ID
     * @return list of SMS notifications
     */
    List<SmsNotification> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, String relatedEntityId);

    /**
     * Finds all SMS notifications for a specific event.
     *
     * @param event event type (ORDER_CREATED, etc.)
     * @return list of SMS notifications
     */
    List<SmsNotification> findByEvent(String event);

    /**
     * Finds SMS notification by Twilio Message SID.
     *
     * @param twilioMessageSid Twilio Message SID
     * @return SMS notification, or null if not found
     */
    SmsNotification findByTwilioMessageSid(String twilioMessageSid);
}

