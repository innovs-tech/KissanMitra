package com.kissanmitra.service;

import com.kissanmitra.entity.SmsNotification;
import com.kissanmitra.entity.User;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.SmsNotificationRepository;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.service.impl.TwilioNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TwilioNotificationService.
 *
 * <p>Tests SMS sending, bulk SMS, and notification methods.
 */
@ExtendWith(MockitoExtension.class)
class TwilioNotificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VleProfileRepository vleProfileRepository;

    @Mock
    private SmsNotificationRepository smsNotificationRepository;

    @InjectMocks
    private TwilioNotificationServiceImpl notificationService;

    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_MESSAGE = "Test message";
    private static final String TEST_USER_ID = "user-id";

    @BeforeEach
    void setUp() {
        // Set up @Value fields and initialize Twilio
        ReflectionTestUtils.setField(notificationService, "twilioAccountSid", "test-sid");
        ReflectionTestUtils.setField(notificationService, "twilioAuthToken", "test-token");
        ReflectionTestUtils.setField(notificationService, "twilioPhoneNumber", "+1234567890");
        
        // Initialize Twilio (this will be mocked in actual tests)
        ReflectionTestUtils.setField(notificationService, "twilioInitialized", false);
        
        // Mock SmsNotificationRepository.save to return notification with ID - use lenient to avoid UnnecessaryStubbingException
        lenient().when(smsNotificationRepository.save(any(SmsNotification.class))).thenAnswer(invocation -> {
            SmsNotification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId("sms-notification-id");
            }
            return notification;
        });
    }

    @Test
    void testSendSms_PhoneNumberNull() {
        // When
        final boolean result = notificationService.sendSms(null, TEST_MESSAGE);

        // Then
        assertFalse(result);
    }

    @Test
    void testSendSms_PhoneNumberEmpty() {
        // When
        final boolean result = notificationService.sendSms("", TEST_MESSAGE);

        // Then
        assertFalse(result);
    }

    @Test
    void testSendSmsWithTracking_NoEvent() {
        // Given - No event/entity info, so no notification record created

        // When
        final boolean result = notificationService.sendSmsWithTracking(
                TEST_PHONE, TEST_MESSAGE, null, null, null, null);

        // Then
        assertFalse(result);
        verify(smsNotificationRepository, never()).save(any(SmsNotification.class));
    }

    @Test
    void testSendBulkSms_EmptyList() {
        // When
        final int result = notificationService.sendBulkSms(List.of(), TEST_MESSAGE);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testSendBulkSms_NullList() {
        // When
        final int result = notificationService.sendBulkSms(null, TEST_MESSAGE);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testFormatPhoneNumber_WithCountryCode() {
        // Given
        final String phoneWithCode = "+919876543210";

        // When - Call sendSms which internally formats phone number
        final boolean result = notificationService.sendSms(phoneWithCode, TEST_MESSAGE);

        // Then - Should handle phone number correctly
        assertFalse(result); // Returns false because Twilio not initialized, but no exception
    }

    @Test
    void testFormatPhoneNumber_WithoutCountryCode() {
        // Given
        final String phoneWithoutCode = "9876543210";

        // When
        final boolean result = notificationService.sendSms(phoneWithoutCode, TEST_MESSAGE);

        // Then
        assertFalse(result); // Returns false because Twilio not initialized
    }

    @Test
    void testNotifyOrderCreated_LeaseOrder() {
        // Given
        final com.kissanmitra.entity.Order order = com.kissanmitra.entity.Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .orderType(com.kissanmitra.domain.enums.OrderType.LEASE)
                .requestedBy(TEST_USER_ID)
                .requestedHours(10.0)
                .build();

        final User admin = User.builder()
                .id("admin-id")
                .phone("+919876543210")
                .build();

        final User requester = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(requester));

        // When
        notificationService.notifyOrderCreated(order);

        // Then - Should attempt to send SMS (will fail due to Twilio not initialized, but method completes)
        verify(userRepository, times(1)).findAllAdmins();
        verify(userRepository, times(1)).findById(TEST_USER_ID);
    }

    @Test
    void testNotifyOrderCreated_RentOrder() {
        // Given
        final com.kissanmitra.entity.Order order = com.kissanmitra.entity.Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .orderType(com.kissanmitra.domain.enums.OrderType.RENT)
                .requestedBy(TEST_USER_ID)
                .handledBy(com.kissanmitra.dto.Handler.builder()
                        .type(com.kissanmitra.domain.enums.HandlerType.VLE)
                        .id("vle-profile-id")
                        .build())
                .build();

        final VleProfile vleProfile = VleProfile.builder()
                .id("vle-profile-id")
                .userId("vle-user-id")
                .build();

        final User vleUser = User.builder()
                .id("vle-user-id")
                .phone("+919876543211")
                .build();

        final User requester = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(vleProfileRepository.findById("vle-profile-id")).thenReturn(Optional.of(vleProfile));
        when(userRepository.findById("vle-user-id")).thenReturn(Optional.of(vleUser));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(requester));

        // When
        notificationService.notifyOrderCreated(order);

        // Then
        verify(vleProfileRepository, times(1)).findById("vle-profile-id");
        verify(userRepository, atLeastOnce()).findById(anyString());
    }

    @Test
    void testNotifyOrderStatusUpdated() {
        // Given
        final com.kissanmitra.entity.Order order = com.kissanmitra.entity.Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .requestedBy(TEST_USER_ID)
                .status(com.kissanmitra.domain.enums.OrderStatus.ACCEPTED)
                .build();

        final User requester = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(requester));

        // When
        notificationService.notifyOrderStatusUpdated(order, com.kissanmitra.domain.enums.OrderStatus.INTEREST_RAISED);

        // Then
        verify(userRepository, times(1)).findById(TEST_USER_ID);
    }

    @Test
    void testNotifyOrderCancelled() {
        // Given
        final com.kissanmitra.entity.Order order = com.kissanmitra.entity.Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .handledBy(com.kissanmitra.dto.Handler.builder()
                        .type(com.kissanmitra.domain.enums.HandlerType.ADMIN)
                        .build())
                .build();

        final User admin = User.builder()
                .id("admin-id")
                .phone("+919876543210")
                .build();

        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        // When
        notificationService.notifyOrderCancelled(order);

        // Then
        verify(userRepository, times(1)).findAllAdmins();
    }

    @Test
    void testNotifyOrderRejected() {
        // Given
        final com.kissanmitra.entity.Order order = com.kissanmitra.entity.Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .requestedBy(TEST_USER_ID)
                .build();

        final User requester = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(requester));

        // When
        notificationService.notifyOrderRejected(order);

        // Then
        verify(userRepository, times(1)).findById(TEST_USER_ID);
    }

    @Test
    void testNotifyLeaseCreated() {
        // Given
        final com.kissanmitra.entity.Lease lease = com.kissanmitra.entity.Lease.builder()
                .id("lease-id")
                .deviceId("device-id")
                .vleId("vle-id")
                .signedByAdminId("admin-id")
                .build();

        final VleProfile vleProfile = VleProfile.builder()
                .id("vle-id")
                .userId("vle-user-id")
                .build();

        final User vleUser = User.builder()
                .id("vle-user-id")
                .phone(TEST_PHONE)
                .build();

        final User admin = User.builder()
                .id("admin-id")
                .phone("+919876543210")
                .build();

        when(vleProfileRepository.findById("vle-id")).thenReturn(Optional.of(vleProfile));
        when(userRepository.findById("vle-user-id")).thenReturn(Optional.of(vleUser));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));

        // When
        notificationService.notifyLeaseCreated(lease);

        // Then
        verify(vleProfileRepository, times(1)).findById("vle-id");
        verify(userRepository, times(2)).findById(anyString());
    }

    @Test
    void testNotifyLeaseStatusUpdated() {
        // Given
        final com.kissanmitra.entity.Lease lease = com.kissanmitra.entity.Lease.builder()
                .id("lease-id")
                .deviceId("device-id")
                .vleId("vle-id")
                .status(com.kissanmitra.domain.enums.LeaseStatus.COMPLETED)
                .build();

        final VleProfile vleProfile = VleProfile.builder()
                .id("vle-id")
                .userId("vle-user-id")
                .build();

        final User vleUser = User.builder()
                .id("vle-user-id")
                .phone(TEST_PHONE)
                .build();

        final User admin = User.builder()
                .id("admin-id")
                .phone("+919876543210")
                .build();

        when(vleProfileRepository.findById("vle-id")).thenReturn(Optional.of(vleProfile));
        when(userRepository.findById("vle-user-id")).thenReturn(Optional.of(vleUser));
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        // When
        notificationService.notifyLeaseStatusUpdated(lease, com.kissanmitra.domain.enums.LeaseStatus.ACTIVE);

        // Then
        verify(vleProfileRepository, times(1)).findById("vle-id");
        verify(userRepository, atLeastOnce()).findAllAdmins();
    }

    @Test
    void testNotifyOperatorAssigned() {
        // Given
        final com.kissanmitra.entity.Lease lease = com.kissanmitra.entity.Lease.builder()
                .id("lease-id")
                .deviceId("device-id")
                .vleId("vle-id")
                .build();

        final User operator = User.builder()
                .id("operator-id")
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findById("operator-id")).thenReturn(Optional.of(operator));

        // When
        notificationService.notifyOperatorAssigned(lease, "operator-id");

        // Then
        verify(userRepository, times(1)).findById("operator-id");
    }
}

