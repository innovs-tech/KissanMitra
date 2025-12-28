package com.kissanmitra.domain.enums;

/**
 * Represents the channel used for delivering notifications.
 *
 * <p>Notifications are delivered via different channels based on:
 * <ul>
 *   <li>User type (Farmers/VLEs use PUSH, Operators use SMS)</li>
 *   <li>Fallback strategy (PUSH failures fallback to SMS)</li>
 * </ul>
 */
public enum NotificationChannel {
    /**
     * Push notification via FCM (Firebase Cloud Messaging).
     * Primary channel for app users (Farmers, VLEs).
     */
    PUSH,

    /**
     * SMS notification via Twilio.
     * Used for operators and as fallback for app users.
     */
    SMS
}

