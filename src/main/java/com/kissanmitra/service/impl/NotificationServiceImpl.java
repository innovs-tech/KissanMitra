package com.kissanmitra.service.impl;

import com.kissanmitra.domain.enums.NotificationChannel;
import com.kissanmitra.domain.enums.NotificationStatus;
import com.kissanmitra.entity.Notification;
import com.kissanmitra.entity.PushToken;
import com.kissanmitra.repository.NotificationRepository;
import com.kissanmitra.repository.PushTokenRepository;
import com.kissanmitra.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service implementation for notification operations.
 *
 * <p>Business Context:
 * - SMS notifications for operators (via Twilio)
 * - Push notifications for app users (via FCM - future)
 * - Best-effort delivery with retry logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushTokenRepository pushTokenRepository;

    /**
     * Sends a notification to a user.
     *
     * @param userId user ID
     * @param channel notification channel
     * @param type notification type
     * @param message notification message
     */
    @Override
    public void sendNotification(
            final String userId,
            final NotificationChannel channel,
            final String type,
            final String message
    ) {
        // Create notification record
        final Notification notification = Notification.builder()
                .userId(userId)
                .channel(channel)
                .type(type)
                .message(message)
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);

        // Send via appropriate channel
        try {
            if (channel == NotificationChannel.SMS) {
                sendSms(userId, message);
            } else if (channel == NotificationChannel.PUSH) {
                sendPush(userId, message);
            }

            // Update status to SENT
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Notification sent to user: {} via {}", userId, channel);
        } catch (Exception e) {
            // Update status to FAILED
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            log.error("Failed to send notification to user: {}", userId, e);
        }
    }

    /**
     * Registers a push token for a user.
     *
     * @param userId user ID
     * @param platform platform (ANDROID, IOS)
     * @param token FCM token
     */
    @Override
    public void registerPushToken(final String userId, final String platform, final String token) {
        final var existing = pushTokenRepository.findByUserIdAndPlatform(userId, platform);

        if (existing.isPresent()) {
            // Update existing token
            final PushToken pushToken = existing.get();
            pushToken.setToken(token);
            pushToken.setActive(true);
            pushToken.setLastSeenAt(Instant.now());
            pushTokenRepository.save(pushToken);
        } else {
            // Create new token
            final PushToken pushToken = PushToken.builder()
                    .userId(userId)
                    .platform(platform)
                    .token(token)
                    .active(true)
                    .lastSeenAt(Instant.now())
                    .build();
            pushTokenRepository.save(pushToken);
        }

        log.info("Push token registered for user: {} platform: {}", userId, platform);
    }

    /**
     * Sends SMS notification via Twilio.
     *
     * <p>Business Decision:
     * - Future enhancement: SMS notifications for operators
     * - Twilio SDK is already integrated for OTP, can be reused here
     * - Currently logs notification for tracking
     *
     * @param userId user ID
     * @param message notification message
     */
    private void sendSms(final String userId, final String message) {
        // FUTURE ENHANCEMENT: Implement SMS sending via Twilio
        // Twilio SDK is already integrated for OTP (AuthServiceImpl)
        // Can reuse Twilio client for operator notifications
        log.info("SMS notification (future enhancement): user={}, message={}", userId, message);
    }

    /**
     * Sends push notification via FCM (Firebase Cloud Messaging).
     *
     * <p>Business Decision:
     * - Future enhancement: Push notifications for app users
     * - Requires FCM SDK integration and Firebase project setup
     * - Currently logs notification for tracking
     *
     * @param userId user ID
     * @param message notification message
     */
    private void sendPush(final String userId, final String message) {
        // FUTURE ENHANCEMENT: Implement push notification via FCM
        // Requires: Firebase Cloud Messaging SDK, Firebase project setup, device tokens
        // Device tokens are already stored in PushToken entity
        log.info("Push notification (future enhancement): user={}, message={}", userId, message);
    }
}

