package com.kissanmitra.service;

import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;

/**
 * Service for sending SMS notifications via Twilio for order and lease events.
 *
 * <p>Business Context:
 * - Notifications sent for order and lease lifecycle events
 * - Phone numbers retrieved from User entity
 * - Separate from general NotificationService (which handles push tokens)
 *
 * <p>Uber Logic:
 * - Validates phone numbers before sending
 * - Handles Twilio API errors gracefully
 * - Logs all notification attempts
 */
public interface SmsNotificationService {

    /**
     * Sends SMS notification to a single phone number.
     *
     * @param phoneNumber recipient phone number
     * @param message message content
     * @return true if sent successfully, false otherwise
     */
    boolean sendSms(String phoneNumber, String message);

    /**
     * Sends SMS notification to multiple phone numbers.
     *
     * @param phoneNumbers list of recipient phone numbers
     * @param message message content
     * @return number of successfully sent messages
     */
    int sendBulkSms(java.util.List<String> phoneNumbers, String message);

    /**
     * Sends order created notification.
     *
     * @param order created order
     */
    void notifyOrderCreated(Order order);

    /**
     * Sends order status update notification.
     *
     * @param order updated order
     * @param previousStatus previous order status
     */
    void notifyOrderStatusUpdated(Order order, OrderStatus previousStatus);

    /**
     * Sends order cancelled notification.
     *
     * @param order cancelled order
     */
    void notifyOrderCancelled(Order order);

    /**
     * Sends order rejected notification.
     *
     * @param order rejected order
     */
    void notifyOrderRejected(Order order);

    /**
     * Sends lease created notification.
     *
     * @param lease created lease
     */
    void notifyLeaseCreated(Lease lease);

    /**
     * Sends lease status update notification.
     *
     * @param lease updated lease
     * @param previousStatus previous lease status
     */
    void notifyLeaseStatusUpdated(Lease lease, LeaseStatus previousStatus);

    /**
     * Sends operator assigned notification.
     *
     * @param lease lease to which operator is assigned
     * @param operatorId operator user ID
     */
    void notifyOperatorAssigned(Lease lease, String operatorId);
}
