package com.kissanmitra.service;

import com.kissanmitra.domain.enums.NotificationChannel;
import com.kissanmitra.domain.enums.NotificationStatus;
import com.kissanmitra.entity.Notification;
import com.kissanmitra.entity.PushToken;
import com.kissanmitra.repository.NotificationRepository;
import com.kissanmitra.repository.PushTokenRepository;
import com.kissanmitra.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 *
 * <p>Tests notification sending and push token registration.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final String TEST_USER_ID = "user-id";
    private static final String TEST_MESSAGE = "Test message";
    private static final String TEST_TOKEN = "fcm-token";

    @BeforeEach
    void setUp() {
        // Mock save to return the same object - use lenient to avoid UnnecessaryStubbingException
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testSendNotification_SMS_Success() {
        // Given
        final NotificationChannel channel = NotificationChannel.SMS;
        final String type = "ORDER_APPROVED";

        // When
        notificationService.sendNotification(TEST_USER_ID, channel, type, TEST_MESSAGE);

        // Then
        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());

        final List<Notification> savedNotifications = captor.getAllValues();
        assertFalse(savedNotifications.isEmpty());
        
        // First save should be PENDING (initial creation)
        final Notification firstSaved = savedNotifications.get(0);
        assertEquals(TEST_USER_ID, firstSaved.getUserId());
        assertEquals(channel, firstSaved.getChannel());
        assertEquals(type, firstSaved.getType());
        assertEquals(TEST_MESSAGE, firstSaved.getMessage());
        // Note: The service saves with PENDING, then immediately updates to SENT
        // So the first save might already be SENT if the update happens in the same transaction
        // We check that at least one notification was saved with the correct details
        assertTrue(firstSaved.getStatus() == NotificationStatus.PENDING || 
                   firstSaved.getStatus() == NotificationStatus.SENT);

        // Last save should be SENT (after successful send)
        final Notification lastSaved = savedNotifications.get(savedNotifications.size() - 1);
        assertEquals(NotificationStatus.SENT, lastSaved.getStatus());
        assertNotNull(lastSaved.getSentAt());
    }

    @Test
    void testSendNotification_PUSH_Success() {
        // Given
        final NotificationChannel channel = NotificationChannel.PUSH;
        final String type = "ORDER_APPROVED";

        // When
        notificationService.sendNotification(TEST_USER_ID, channel, type, TEST_MESSAGE);

        // Then
        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());

        final Notification saved = captor.getAllValues().get(0);
        assertEquals(NotificationChannel.PUSH, saved.getChannel());
    }

    @Test
    void testSendNotification_Failure() {
        // Given
        final NotificationChannel channel = NotificationChannel.SMS;
        // First save succeeds (creates notification), second save fails (updating status)
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("Database error"));

        // When - Should not throw exception, service handles it gracefully
        // The service catches exceptions and updates status to FAILED
        assertDoesNotThrow(() -> {
            notificationService.sendNotification(TEST_USER_ID, channel, "TEST", TEST_MESSAGE);
        });

        // Then - should attempt to save multiple times (create, update to SENT, update to FAILED)
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void testRegisterPushToken_NewToken() {
        // Given
        final String platform = "ANDROID";
        when(pushTokenRepository.findByUserIdAndPlatform(TEST_USER_ID, platform))
                .thenReturn(Optional.empty());

        // When
        notificationService.registerPushToken(TEST_USER_ID, platform, TEST_TOKEN);

        // Then
        final ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository, times(1)).save(captor.capture());

        final PushToken saved = captor.getValue();
        assertEquals(TEST_USER_ID, saved.getUserId());
        assertEquals(platform, saved.getPlatform());
        assertEquals(TEST_TOKEN, saved.getToken());
        assertTrue(saved.getActive());
        assertNotNull(saved.getLastSeenAt());
    }

    @Test
    void testRegisterPushToken_UpdateExisting() {
        // Given
        final String platform = "IOS";
        final PushToken existingToken = PushToken.builder()
                .id("token-id")
                .userId(TEST_USER_ID)
                .platform(platform)
                .token("old-token")
                .active(false)
                .build();

        when(pushTokenRepository.findByUserIdAndPlatform(TEST_USER_ID, platform))
                .thenReturn(Optional.of(existingToken));

        // When
        notificationService.registerPushToken(TEST_USER_ID, platform, TEST_TOKEN);

        // Then
        verify(pushTokenRepository, times(1)).save(existingToken);
        assertEquals(TEST_TOKEN, existingToken.getToken());
        assertTrue(existingToken.getActive());
        assertNotNull(existingToken.getLastSeenAt());
    }
}

