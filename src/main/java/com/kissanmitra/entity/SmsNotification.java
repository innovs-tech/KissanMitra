package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity for tracking SMS notifications sent via Twilio.
 *
 * <p>Business Context:
 * - Provides audit trail for all SMS notifications
 * - Stores Twilio Message SID for verification/proof
 * - Tracks delivery status and errors
 * - Links notifications to related entities (Order, Lease)
 *
 * <p>Uber Logic:
 * - Created before sending SMS (status: PENDING)
 * - Updated after Twilio response (status: SENT or FAILED)
 * - Stores Twilio Message SID for proof/verification
 * - Enables customer support to verify SMS delivery
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "sms_notifications")
public class SmsNotification extends BaseEntity {

    /**
     * Recipient phone number (E.164 format, e.g., +919876543210).
     */
    @Indexed
    private String to;

    /**
     * SMS message content.
     */
    private String message;

    /**
     * Event that triggered this notification.
     * Examples: ORDER_CREATED, ORDER_STATUS_UPDATED, ORDER_CANCELLED,
     *           ORDER_REJECTED, LEASE_CREATED, LEASE_STATUS_UPDATED, OPERATOR_ASSIGNED
     */
    private String event;

    /**
     * Related entity type (ORDER, LEASE).
     */
    private String relatedEntityType;

    /**
     * Related entity ID (orderId, leaseId).
     */
    @Indexed
    private String relatedEntityId;

    /**
     * User ID of recipient (if available).
     */
    @Indexed
    private String userId;

    /**
     * Twilio Message SID (unique identifier from Twilio for verification/proof).
     * Format: SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     */
    private String twilioMessageSid;

    /**
     * Delivery status (PENDING, SENT, FAILED).
     */
    private NotificationStatus status;

    /**
     * Error message if delivery failed.
     */
    private String errorMessage;

    /**
     * Timestamp when SMS was sent (set when status changes to SENT).
     */
    private Instant sentAt;

    /**
     * Cost of SMS (if available from Twilio response).
     */
    private String cost;
}

