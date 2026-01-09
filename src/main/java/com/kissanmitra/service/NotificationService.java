package com.kissanmitra.service;

import com.kissanmitra.domain.enums.NotificationChannel;

/**
 * Service for general notification operations (push tokens, general notifications).
 *
 * <p>Business Context:
 * - Handles push token registration for app users
 * - Sends notifications via different channels (SMS, PUSH)
 * - Separate from SmsNotificationService (which handles order/lease specific SMS)
 */
public interface NotificationService {

    /**
     * Sends a notification to a user.
     *
     * @param userId user ID
     * @param channel notification channel
     * @param type notification type
     * @param message notification message
     */
    void sendNotification(String userId, NotificationChannel channel, String type, String message);

    /**
     * Registers a push token for a user.
     *
     * @param userId user ID
     * @param platform platform (ANDROID, IOS)
     * @param token FCM token
     */
    void registerPushToken(String userId, String platform, String token);
}
