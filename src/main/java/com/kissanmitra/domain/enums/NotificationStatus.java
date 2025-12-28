package com.kissanmitra.domain.enums;

/**
 * Represents the delivery status of a notification.
 *
 * <p>Used to track notification lifecycle and enable retry logic.
 */
public enum NotificationStatus {
    /**
     * Notification is queued and pending delivery.
     */
    PENDING,

    /**
     * Notification has been successfully delivered.
     */
    SENT,

    /**
     * Notification delivery failed.
     * May trigger retry or fallback to alternative channel.
     */
    FAILED
}

