package com.kissanmitra.service.impl;

import com.kissanmitra.domain.enums.HandlerType;
import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.domain.enums.NotificationStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.SmsNotification;
import com.kissanmitra.entity.User;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.SmsNotificationRepository;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.service.SmsNotificationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Twilio SMS notification service implementation.
 *
 * <p>Business Decision:
 * - Uses Twilio API for SMS delivery
 * - Phone numbers must be in E.164 format (+1234567890)
 * - Notifications are sent asynchronously (fire-and-forget)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioNotificationServiceImpl implements SmsNotificationService {

    @Value("${twilio.accountSid:}")
    private String twilioAccountSid;

    @Value("${twilio.authToken:}")
    private String twilioAuthToken;

    @Value("${twilio.fromNumber:}")
    private String twilioPhoneNumber;

    private final UserRepository userRepository;
    private final VleProfileRepository vleProfileRepository;
    private final SmsNotificationRepository smsNotificationRepository;

    private static final String DEFAULT_COUNTRY_CODE = "+91";
    private static boolean twilioInitialized = false;

    /**
     * Initializes Twilio SDK once at application startup.
     *
     * <p>Business Decision:
     * - Twilio.init() should be called once, not per message
     * - If credentials are missing, SMS will be logged but not sent
     */
    @PostConstruct
    public void initTwilio() {
        if (twilioAccountSid != null && !twilioAccountSid.isEmpty() &&
            twilioAuthToken != null && !twilioAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                twilioInitialized = true;
                log.info("Twilio initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage(), e);
                twilioInitialized = false;
            }
        } else {
            log.warn("Twilio credentials not configured. SMS notifications will be logged only.");
            twilioInitialized = false;
        }
    }

    /**
     * Formats phone number to E.164 format.
     *
     * <p>Business Decision:
     * - Adds +91 prefix if phone number doesn't start with +
     * - Same logic as AuthServiceImpl for consistency
     *
     * @param phoneNumber raw phone number
     * @return formatted phone number in E.164 format
     */
    private String formatPhoneNumber(final String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        final String trimmed = phoneNumber.trim();
        // If already has country code (starts with +), return as is
        if (trimmed.startsWith("+")) {
            return trimmed;
        }

        // BUSINESS DECISION: Add +91 prefix for Indian numbers (default country code)
        return DEFAULT_COUNTRY_CODE + trimmed;
    }

    @Override
    public boolean sendSms(final String phoneNumber, final String message) {
        // Call overloaded method without tracking (for backward compatibility)
        return sendSmsWithTracking(phoneNumber, message, null, null, null, null);
    }

    /**
     * Sends SMS with tracking in database.
     *
     * <p>Business Decision:
     * - Creates SmsNotification record before sending (status: PENDING)
     * - Updates record after Twilio response (status: SENT or FAILED)
     * - Stores Twilio Message SID for proof/verification
     *
     * @param phoneNumber recipient phone number
     * @param message message content
     * @param event event that triggered this notification (e.g., ORDER_CREATED)
     * @param relatedEntityType related entity type (ORDER, LEASE)
     * @param relatedEntityId related entity ID (orderId, leaseId)
     * @param userId user ID of recipient (if available)
     * @return true if sent successfully, false otherwise
     */
    public boolean sendSmsWithTracking(
            final String phoneNumber,
            final String message,
            final String event,
            final String relatedEntityType,
            final String relatedEntityId,
            final String userId
    ) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.warn("Cannot send SMS: phone number is null or empty");
            return false;
        }

        // Format phone number to E.164 format
        final String formattedPhone = formatPhoneNumber(phoneNumber);

        // Create notification record with PENDING status
        SmsNotification notification = null;
        if (event != null || relatedEntityType != null) {
            notification = SmsNotification.builder()
                    .to(formattedPhone)
                    .message(message)
                    .event(event)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .userId(userId)
                    .status(NotificationStatus.PENDING)
                    .build();
            notification = smsNotificationRepository.save(notification);
            log.debug("Created SMS notification record with ID: {} for event: {}", notification.getId(), event);
        }

        // Validate Twilio configuration
        if (!twilioInitialized) {
            log.warn("Twilio not initialized. Logging SMS notification only: [{}]: {}", phoneNumber, message);
            if (notification != null) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage("Twilio not initialized");
                smsNotificationRepository.save(notification);
            }
            return false;
        }

        if (twilioPhoneNumber == null || twilioPhoneNumber.isEmpty()) {
            log.warn("Twilio from number not configured. Cannot send SMS to: {}", phoneNumber);
            if (notification != null) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage("Twilio from number not configured");
                smsNotificationRepository.save(notification);
            }
            return false;
        }

        try {
            final String formattedFrom = formatPhoneNumber(twilioPhoneNumber);

            // Send SMS via Twilio
            final Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedPhone),      // To
                    new PhoneNumber(formattedFrom),       // From
                    message                               // Message body
            ).create();

            // Update notification record with success details
            if (notification != null) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setTwilioMessageSid(twilioMessage.getSid());
                notification.setSentAt(Instant.now());
                // Extract cost from Twilio response if available
                // Note: Twilio Message object may not have getPrice() method in all SDK versions
                // Cost can be retrieved from Twilio API separately if needed
                smsNotificationRepository.save(notification);
            }

            log.info("SMS sent successfully to {} via Twilio. Message SID: {}", 
                    formattedPhone, twilioMessage.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            
            // Update notification record with failure details
            if (notification != null) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                smsNotificationRepository.save(notification);
            }
            return false;
        }
    }

    @Override
    public int sendBulkSms(final List<String> phoneNumbers, final String message) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String phoneNumber : phoneNumbers) {
            if (sendSms(phoneNumber, message)) {
                successCount++;
            }
        }

        log.info("Sent {}/{} SMS notifications", successCount, phoneNumbers.size());
        return successCount;
    }

    /**
     * Sends bulk SMS with tracking.
     *
     * @param phoneNumbers list of recipient phone numbers
     * @param message message content
     * @param event event that triggered this notification
     * @param relatedEntityType related entity type
     * @param relatedEntityId related entity ID
     * @return number of successfully sent messages
     */
    private int sendBulkSmsWithTracking(
            final List<String> phoneNumbers,
            final String message,
            final String event,
            final String relatedEntityType,
            final String relatedEntityId
    ) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String phoneNumber : phoneNumbers) {
            // Get userId from phone number if available
            String userId = null;
            final User user = userRepository.findByPhone(phoneNumber);
            if (user != null) {
                userId = user.getId();
            }

            if (sendSmsWithTracking(phoneNumber, message, event, relatedEntityType, relatedEntityId, userId)) {
                successCount++;
            }
        }

        log.info("Sent {}/{} SMS notifications with tracking", successCount, phoneNumbers.size());
        return successCount;
    }

    @Override
    public void notifyOrderCreated(final Order order) {
        final String event = "ORDER_CREATED";
        final String relatedEntityType = "ORDER";
        final String relatedEntityId = order.getId();

        if (order.getOrderType() == OrderType.LEASE) {
            // LEASE order: Notify ADMIN + VLE (requester) with different messages
            // Get all admin phone numbers
            final List<User> admins = userRepository.findAllAdmins();
            final List<String> adminPhones = admins.stream()
                    .map(User::getPhone)
                    .filter(phone -> phone != null && !phone.isEmpty())
                    .collect(Collectors.toList());

            // Get VLE (requester) phone number
            final User requester = userRepository.findById(order.getRequestedBy())
                    .orElse(null);

            // Send different messages to different roles
            if (!adminPhones.isEmpty()) {
                final String adminMessage = buildLeaseOrderCreatedMessageForAdmin(order);
                sendBulkSmsWithTracking(adminPhones, adminMessage, event, relatedEntityType, relatedEntityId);
            }

            if (requester != null && requester.getPhone() != null) {
                final String requesterMessage = buildLeaseOrderCreatedMessageForRequester(order);
                sendSmsWithTracking(requester.getPhone(), requesterMessage, event, relatedEntityType, relatedEntityId, requester.getId());
            }
        } else {
            // RENT order: Notify VLE (handler) + FARMER (requester) with different messages
            // Get VLE phone number from handler
            final Handler handler = order.getHandledBy();
            String vlePhone = null;
            String vleUserId = null;
            if (handler != null && handler.getId() != null) {
                final VleProfile vleProfile = vleProfileRepository.findById(handler.getId())
                        .orElse(null);
                if (vleProfile != null) {
                    final User vleUser = userRepository.findById(vleProfile.getUserId())
                            .orElse(null);
                    if (vleUser != null && vleUser.getPhone() != null) {
                        vlePhone = vleUser.getPhone();
                        vleUserId = vleUser.getId();
                    }
                }
            }

            // Get FARMER (requester) phone number
            final User requester = userRepository.findById(order.getRequestedBy())
                    .orElse(null);

            // Send different messages to different roles
            if (vlePhone != null) {
                final String vleMessage = buildRentOrderCreatedMessageForHandler(order);
                sendSmsWithTracking(vlePhone, vleMessage, event, relatedEntityType, relatedEntityId, vleUserId);
            }

            if (requester != null && requester.getPhone() != null) {
                final String requesterMessage = buildRentOrderCreatedMessageForRequester(order);
                sendSmsWithTracking(requester.getPhone(), requesterMessage, event, relatedEntityType, relatedEntityId, requester.getId());
            }
        }
    }

    @Override
    public void notifyOrderStatusUpdated(final Order order, final OrderStatus previousStatus) {
        final String event = "ORDER_STATUS_UPDATED";
        final String relatedEntityType = "ORDER";
        final String relatedEntityId = order.getId();

        // Notify requester with requester-specific message
        final User requester = userRepository.findById(order.getRequestedBy())
                .orElse(null);
        if (requester != null && requester.getPhone() != null) {
            final String requesterMessage = buildOrderStatusUpdatedMessageForRequester(order, previousStatus);
            sendSmsWithTracking(requester.getPhone(), requesterMessage, event, relatedEntityType, relatedEntityId, requester.getId());
        }

        // Notify handler with handler-specific message
        final Handler handler = order.getHandledBy();
        if (handler != null) {
            if (handler.getType() == HandlerType.ADMIN) {
                // Get all admin phone numbers
                final List<User> admins = userRepository.findAllAdmins();
                final List<String> adminPhones = admins.stream()
                        .map(User::getPhone)
                        .filter(phone -> phone != null && !phone.isEmpty())
                        .collect(Collectors.toList());
                if (!adminPhones.isEmpty()) {
                    final String adminMessage = buildOrderStatusUpdatedMessageForHandler(order, previousStatus);
                    sendBulkSmsWithTracking(adminPhones, adminMessage, event, relatedEntityType, relatedEntityId);
                }
            } else if (handler.getType() == HandlerType.VLE) {
                // Get VLE phone number
                final VleProfile vleProfile = vleProfileRepository.findById(handler.getId())
                        .orElse(null);
                if (vleProfile != null) {
                    final User vleUser = userRepository.findById(vleProfile.getUserId())
                            .orElse(null);
                    if (vleUser != null && vleUser.getPhone() != null) {
                        final String vleMessage = buildOrderStatusUpdatedMessageForHandler(order, previousStatus);
                        sendSmsWithTracking(vleUser.getPhone(), vleMessage, event, relatedEntityType, relatedEntityId, vleUser.getId());
                    }
                }
            }
        }
    }

    @Override
    public void notifyOrderCancelled(final Order order) {
        final String event = "ORDER_CANCELLED";
        final String relatedEntityType = "ORDER";
        final String relatedEntityId = order.getId();

        // Notify handler
        final List<String> recipients = new ArrayList<>();
        final Handler handler = order.getHandledBy();
        
        if (handler != null) {
            if (handler.getType() == HandlerType.ADMIN) {
                final List<User> admins = userRepository.findAllAdmins();
                recipients.addAll(admins.stream()
                        .map(User::getPhone)
                        .filter(phone -> phone != null && !phone.isEmpty())
                        .collect(Collectors.toList()));
            } else if (handler.getType() == HandlerType.VLE) {
                final VleProfile vleProfile = vleProfileRepository.findById(handler.getId())
                        .orElse(null);
                if (vleProfile != null) {
                    final User vleUser = userRepository.findById(vleProfile.getUserId())
                            .orElse(null);
                    if (vleUser != null && vleUser.getPhone() != null) {
                        recipients.add(vleUser.getPhone());
                    }
                }
            }
        }

        final String message = buildOrderCancelledMessage(order);
        sendBulkSmsWithTracking(recipients, message, event, relatedEntityType, relatedEntityId);
    }

    @Override
    public void notifyOrderRejected(final Order order) {
        final String event = "ORDER_REJECTED";
        final String relatedEntityType = "ORDER";
        final String relatedEntityId = order.getId();

        // Notify requester
        final User requester = userRepository.findById(order.getRequestedBy())
                .orElse(null);
        if (requester != null && requester.getPhone() != null) {
            final String message = buildOrderRejectedMessage(order);
            sendSmsWithTracking(requester.getPhone(), message, event, relatedEntityType, relatedEntityId, requester.getId());
        }
    }

    @Override
    public void notifyLeaseCreated(final Lease lease) {
        final String event = "LEASE_CREATED";
        final String relatedEntityType = "LEASE";
        final String relatedEntityId = lease.getId();

        // Notify VLE (lease owner) with VLE-specific message
        final VleProfile vleProfile = vleProfileRepository.findById(lease.getVleId())
                .orElse(null);
        if (vleProfile != null) {
            final User vleUser = userRepository.findById(vleProfile.getUserId())
                    .orElse(null);
            if (vleUser != null && vleUser.getPhone() != null) {
                final String vleMessage = buildLeaseCreatedMessageForVle(lease);
                sendSmsWithTracking(vleUser.getPhone(), vleMessage, event, relatedEntityType, relatedEntityId, vleUser.getId());
            }
        }

        // Notify ADMIN (who created the lease) with admin-specific message
        if (lease.getSignedByAdminId() != null) {
            final User admin = userRepository.findById(lease.getSignedByAdminId())
                    .orElse(null);
            if (admin != null && admin.getPhone() != null) {
                final String adminMessage = buildLeaseCreatedMessageForAdmin(lease);
                sendSmsWithTracking(admin.getPhone(), adminMessage, event, relatedEntityType, relatedEntityId, admin.getId());
            }
        }
    }

    @Override
    public void notifyLeaseStatusUpdated(final Lease lease, final LeaseStatus previousStatus) {
        final String event = "LEASE_STATUS_UPDATED";
        final String relatedEntityType = "LEASE";
        final String relatedEntityId = lease.getId();

        // Notify VLE with VLE-specific message
        final VleProfile vleProfile = vleProfileRepository.findById(lease.getVleId())
                .orElse(null);
        if (vleProfile != null) {
            final User vleUser = userRepository.findById(vleProfile.getUserId())
                    .orElse(null);
            if (vleUser != null && vleUser.getPhone() != null) {
                final String vleMessage = buildLeaseStatusUpdatedMessageForVle(lease, previousStatus);
                sendSmsWithTracking(vleUser.getPhone(), vleMessage, event, relatedEntityType, relatedEntityId, vleUser.getId());
            }
        }

        // Notify all admins with admin-specific message
        final List<User> admins = userRepository.findAllAdmins();
        final List<String> adminPhones = admins.stream()
                .map(User::getPhone)
                .filter(phone -> phone != null && !phone.isEmpty())
                .collect(Collectors.toList());
        if (!adminPhones.isEmpty()) {
            final String adminMessage = buildLeaseStatusUpdatedMessageForAdmin(lease, previousStatus);
            sendBulkSmsWithTracking(adminPhones, adminMessage, event, relatedEntityType, relatedEntityId);
        }
    }

    @Override
    public void notifyOperatorAssigned(final Lease lease, final String operatorId) {
        final String event = "OPERATOR_ASSIGNED";
        final String relatedEntityType = "LEASE";
        final String relatedEntityId = lease.getId();

        final User operator = userRepository.findById(operatorId)
                .orElse(null);
        if (operator != null && operator.getPhone() != null) {
            final String message = buildOperatorAssignedMessage(lease, operatorId);
            sendSmsWithTracking(operator.getPhone(), message, event, relatedEntityType, relatedEntityId, operator.getId());
        }
    }

    // Message builders - Order Created (LEASE)
    private String buildLeaseOrderCreatedMessageForAdmin(final Order order) {
        return String.format(
                "New LEASE order #%s created by VLE. Device: %s, Requested: %s hours / %s acres. Please review.",
                order.getId().substring(0, 8),
                order.getDeviceId().substring(0, 8),
                order.getRequestedHours() != null ? order.getRequestedHours() : "N/A",
                order.getRequestedAcres() != null ? order.getRequestedAcres() : "N/A"
        );
    }

    private String buildLeaseOrderCreatedMessageForRequester(final Order order) {
        return String.format(
                "Your LEASE order #%s has been submitted successfully. Device: %s, Requested: %s hours / %s acres. We'll review it shortly.",
                order.getId().substring(0, 8),
                order.getDeviceId().substring(0, 8),
                order.getRequestedHours() != null ? order.getRequestedHours() : "N/A",
                order.getRequestedAcres() != null ? order.getRequestedAcres() : "N/A"
        );
    }

    // Message builders - Order Created (RENT)
    private String buildRentOrderCreatedMessageForHandler(final Order order) {
        return String.format(
                "New RENT order #%s from Farmer. Device: %s, Requested: %s hours / %s acres. Please review.",
                order.getId().substring(0, 8),
                order.getDeviceId().substring(0, 8),
                order.getRequestedHours() != null ? order.getRequestedHours() : "N/A",
                order.getRequestedAcres() != null ? order.getRequestedAcres() : "N/A"
        );
    }

    private String buildRentOrderCreatedMessageForRequester(final Order order) {
        return String.format(
                "Your RENT order #%s has been submitted successfully. Device: %s, Requested: %s hours / %s acres. VLE will review it shortly.",
                order.getId().substring(0, 8),
                order.getDeviceId().substring(0, 8),
                order.getRequestedHours() != null ? order.getRequestedHours() : "N/A",
                order.getRequestedAcres() != null ? order.getRequestedAcres() : "N/A"
        );
    }

    // Message builders - Order Status Updated
    private String buildOrderStatusUpdatedMessageForRequester(final Order order, final OrderStatus previousStatus) {
        return String.format(
                "Your order #%s status updated: %s → %s",
                order.getId().substring(0, 8),
                previousStatus,
                order.getStatus()
        );
    }

    private String buildOrderStatusUpdatedMessageForHandler(final Order order, final OrderStatus previousStatus) {
        return String.format(
                "Order #%s you're handling status updated: %s → %s",
                order.getId().substring(0, 8),
                previousStatus,
                order.getStatus()
        );
    }

    private String buildOrderCancelledMessage(final Order order) {
        return String.format(
                "Order #%s has been cancelled by the requester.",
                order.getId().substring(0, 8)
        );
    }

    private String buildOrderRejectedMessage(final Order order) {
        return String.format(
                "Your order #%s has been rejected. Reason: %s",
                order.getId().substring(0, 8),
                order.getNote() != null ? order.getNote() : "No reason provided"
        );
    }

    // Message builders - Lease Created
    private String buildLeaseCreatedMessageForVle(final Lease lease) {
        return String.format(
                "Your lease #%s has been created successfully! Device: %s, Start: %s, End: %s. You can now start operations.",
                lease.getId().substring(0, 8),
                lease.getDeviceId().substring(0, 8),
                lease.getStartDate(),
                lease.getEndDate()
        );
    }

    private String buildLeaseCreatedMessageForAdmin(final Lease lease) {
        return String.format(
                "Lease #%s created successfully for VLE. Device: %s, Start: %s, End: %s",
                lease.getId().substring(0, 8),
                lease.getDeviceId().substring(0, 8),
                lease.getStartDate(),
                lease.getEndDate()
        );
    }

    // Message builders - Lease Status Updated
    private String buildLeaseStatusUpdatedMessageForVle(final Lease lease, final LeaseStatus previousStatus) {
        return String.format(
                "Your lease #%s status updated: %s → %s",
                lease.getId().substring(0, 8),
                previousStatus,
                lease.getStatus()
        );
    }

    private String buildLeaseStatusUpdatedMessageForAdmin(final Lease lease, final LeaseStatus previousStatus) {
        return String.format(
                "Lease #%s status updated: %s → %s",
                lease.getId().substring(0, 8),
                previousStatus,
                lease.getStatus()
        );
    }

    private String buildOperatorAssignedMessage(final Lease lease, final String operatorId) {
        return String.format(
                "You have been assigned as operator to Lease #%s. Device: %s",
                lease.getId().substring(0, 8),
                lease.getDeviceId().substring(0, 8)
        );
    }
}

