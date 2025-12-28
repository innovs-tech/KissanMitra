package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.NotificationChannel;
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
 * Notification entity for tracking notification delivery.
 *
 * <p>Business Context:
 * - Notifications sent via SMS (operators) or Push (app users)
 * - Delivery status tracked for retry logic
 * - Event-driven notifications on lifecycle events
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "notifications")
public class Notification extends BaseEntity {

    /**
     * User ID who receives the notification.
     */
    @Indexed
    private String userId;

    /**
     * Notification channel (PUSH or SMS).
     */
    private NotificationChannel channel;

    /**
     * Notification type (ORDER_APPROVED, etc.).
     */
    private String type;

    /**
     * Notification message.
     */
    private String message;

    /**
     * Delivery status.
     */
    private NotificationStatus status;

    /**
     * Timestamp when notification was sent.
     */
    private Instant sentAt;
}

