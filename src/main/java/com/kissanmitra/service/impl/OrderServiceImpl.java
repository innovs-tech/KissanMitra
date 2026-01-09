package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.HandlerType;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.User;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.repository.LeaseRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.request.CreateOrderRequest;
import com.kissanmitra.request.UpdateOrderStatusRequest;
import com.kissanmitra.service.AuditService;
import com.kissanmitra.service.OrderService;
import com.kissanmitra.service.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for order management.
 *
 * <p>Business Context:
 * - Orders represent intent (separate from Leases which represent execution)
 * - Order type determined by requester role (FARMER → RENT, VLE → LEASE)
 * - Orders are routed to Admin (LEASE) or VLE (RENT)
 * - State machine enforces valid transitions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String INTENT_STATUS_CONSUMED = "CONSUMED";

    private final OrderRepository orderRepository;
    private final DeviceRepository deviceRepository;
    private final DiscoveryIntentRepository discoveryIntentRepository;
    private final VleProfileRepository vleProfileRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;
    private final AuditService auditService;
    private final com.kissanmitra.service.SmsNotificationService smsNotificationService;

    /**
     * Creates a new order.
     *
     * <p>Business Decision:
     * - Order type determined by requester role (FARMER → RENT, VLE → LEASE)
     * - Handler set based on order type (ADMIN for LEASE, VLE for RENT)
     * - Status set to INTEREST_RAISED
     * - RENT orders require device to be leased
     * - LEASE orders require device to NOT be leased
     *
     * @param request order creation request
     * @return created order
     */
    @Override
    @Transactional
    public Order createOrder(final CreateOrderRequest request) {
        // Validate device exists
        final Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // BUSINESS DECISION: Only LIVE devices can be ordered
        if (device.getStatus() != com.kissanmitra.domain.enums.DeviceStatus.LIVE) {
            throw new RuntimeException("Device is not available for ordering");
        }

        // BUSINESS DECISION: Order type determined by requester role
        // FARMER → RENT order, VLE → LEASE order
        final String currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User must be authenticated to create order");
        }

        final UserRole activeRole = userContext.getCurrentUserActiveRole();
        if (activeRole == null) {
            throw new RuntimeException("User must have an active role to create order");
        }

        final OrderType orderType;
        if (activeRole == UserRole.FARMER) {
            orderType = OrderType.RENT;
            // BUSINESS DECISION: RENT orders require device to be leased
            if (device.getCurrentLeaseId() == null) {
                throw new RuntimeException("Device must be leased to a VLE for RENT orders");
            }
        } else if (activeRole == UserRole.VLE) {
            orderType = OrderType.LEASE;
            // BUSINESS DECISION: LEASE orders require device to NOT be leased
            if (device.getCurrentLeaseId() != null) {
                throw new RuntimeException("Device is already leased. Cannot create LEASE order for leased device");
            }
            
        } else {
            throw new RuntimeException("Only FARMER and VLE roles can create orders");
        }

        // Determine handler based on order type
        final Handler handler = determineHandler(orderType, device);

        // BUSINESS DECISION: Get user details for phone and name
        final User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        final String phone = userContext.getCurrentUserPhone();
        final String name = user.getProfile() != null ? user.getProfile().getName() : null;

        // Create order
        final Order order = Order.builder()
                .orderType(orderType)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(request.getDeviceId())
                .requestedBy(currentUserId)
                .handledBy(handler)
                .requestedHours(request.getRequestedHours())
                .requestedAcres(request.getRequestedAcres())
                .note(request.getNote())
                .phone(phone)
                .name(name)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        final Order saved = orderRepository.save(order);

        // Mark intent as consumed if order was created from intent
        if (request.getIntentId() != null) {
            final DiscoveryIntent intent = discoveryIntentRepository.findById(request.getIntentId())
                    .orElse(null);
            if (intent != null) {
                intent.setStatus(INTENT_STATUS_CONSUMED);
                discoveryIntentRepository.save(intent);
            }
        }

        log.info("Created order: {} of type: {} for device: {} by user: {}", 
                saved.getId(), orderType, request.getDeviceId(), currentUserId);
        
        // Send notification
        smsNotificationService.notifyOrderCreated(saved);
        
        return saved;
    }

    /**
     * Gets order by ID.
     *
     * @param id order ID
     * @return order
     */
    @Override
    public Order getOrderById(final String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Gets orders for current user.
     *
     * @return list of orders
     */
    @Override
    public List<Order> getMyOrders() {
        final String userId = userContext.getCurrentUserId();
        // Get all orders for user (regardless of status)
        return orderRepository.findAll().stream()
                .filter(order -> order.getRequestedBy().equals(userId))
                .toList();
    }

    /**
     * Updates order status.
     *
     * <p>Business Decision:
     * - Validates state transition using OrderStateMachine
     * - Only handler can update order status
     *
     * @param id order ID
     * @param request status update request
     * @return updated order
     */
    @Override
    @Transactional
    public Order updateOrderStatus(final String id, final UpdateOrderStatusRequest request) {
        final Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Validate state transition
        if (!OrderStateMachine.canTransition(order.getStatus(), request.getToState())) {
            throw new RuntimeException(
                    String.format("Invalid state transition from %s to %s", order.getStatus(), request.getToState())
            );
        }

        // BUSINESS DECISION: Only handler can update order status
        validateHandlerAccess(order);

        // Update status
        final Order updated = order.toBuilder()
                .status(request.getToState())
                .note(request.getNote() != null ? request.getNote() : order.getNote())
                .build();

        final Order saved = orderRepository.save(updated);
        log.info("Updated order: {} status from {} to {}", id, order.getStatus(), request.getToState());

        // Trigger audit logging
        auditService.logEvent(
                "ORDER",
                id,
                "STATUS_CHANGED",
                order.getStatus().name(),
                request.getToState().name(),
                request.getNote()
        );

        // Send notification
        smsNotificationService.notifyOrderStatusUpdated(saved, order.getStatus());

        return saved;
    }

    /**
     * Determines the handler for an order based on order type.
     *
     * <p>Business Decision:
     * - LEASE orders: Handled by Admin (use current user if they have ADMIN role)
     * - RENT orders: Handled by VLE who has the device on lease
     *
     * @param orderType order type (LEASE or RENT)
     * @param device device for which order is placed
     * @return handler for the order
     */
    private Handler determineHandler(final OrderType orderType, final Device device) {
        if (orderType == OrderType.LEASE) {
            // BUSINESS DECISION: LEASE orders handled by Admin
            // VLE is the requester, so handler is ADMIN (any admin can handle)
            // Use placeholder "admin" as handler ID since validation checks role, not specific ID
            return Handler.builder()
                    .type(HandlerType.ADMIN)
                    .id("admin")
                    .build();
        } else {
            // BUSINESS DECISION: RENT orders handled by VLE (device owner)
            // Get VLE from device's current lease
            if (device.getCurrentLeaseId() == null) {
                throw new RuntimeException("Device must be leased to a VLE for RENT orders");
            }

            final Lease lease = leaseRepository.findById(device.getCurrentLeaseId())
                    .orElseThrow(() -> new RuntimeException("Lease not found for device"));

            if (lease.getVleId() == null) {
                throw new RuntimeException("VLE ID not found in lease");
            }

            return Handler.builder()
                    .type(HandlerType.VLE)
                    .id(lease.getVleId())
                    .build();
        }
    }

    /**
     * Validates that the current user is authorized to update the order.
     *
     * <p>Business Decision:
     * - For ADMIN handler: User must have ADMIN role
     * - For VLE handler: User must be the VLE (check via VleProfile)
     *
     * @param order order to validate
     */
    private void validateHandlerAccess(final Order order) {
        final String currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        final Handler handler = order.getHandledBy();
        if (handler == null) {
            throw new RuntimeException("Order handler not set");
        }

        if (handler.getType() == HandlerType.ADMIN) {
            // BUSINESS DECISION: For ADMIN handler, user must have ADMIN role
            if (!userContext.hasRole(UserRole.ADMIN) && !userContext.hasRole(UserRole.SUPER_ADMIN)) {
                throw new RuntimeException("Only admins can handle LEASE orders");
            }
        } else if (handler.getType() == HandlerType.VLE) {
            // BUSINESS DECISION: For VLE handler, user must be the VLE
            // Handler ID is the VLE profile ID (from lease.vleId)
            final VleProfile vleProfile = vleProfileRepository.findByUserId(currentUserId)
                    .orElseThrow(() -> new RuntimeException("VLE profile not found for user"));

            // Handler ID should match VLE profile ID
            if (!handler.getId().equals(vleProfile.getId())) {
                throw new RuntimeException("Only the assigned VLE can handle this RENT order");
            }
        } else {
            throw new RuntimeException("Unknown handler type: " + handler.getType());
        }
    }

    /**
     * Cancels an order by requester.
     *
     * <p>Business Decision:
     * - Only requester can cancel their own order
     * - Can only cancel if status is INTEREST_RAISED
     *
     * @param id order ID
     * @param note cancellation note
     * @return cancelled order
     */
    @Override
    @Transactional
    public Order cancelOrder(final String id, final String note) {
        final Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // BUSINESS DECISION: Only requester can cancel
        final String currentUserId = userContext.getCurrentUserId();
        if (!order.getRequestedBy().equals(currentUserId)) {
            throw new RuntimeException("Only the requester can cancel the order");
        }

        // BUSINESS DECISION: Can only cancel if status is INTEREST_RAISED
        if (order.getStatus() != OrderStatus.INTEREST_RAISED) {
            throw new RuntimeException("Order can only be cancelled when status is INTEREST_RAISED");
        }

        // Validate state transition
        if (!OrderStateMachine.canTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            throw new RuntimeException("Cannot cancel order in current status");
        }

        // Update status to CANCELLED
        final Order updated = order.toBuilder()
                .status(OrderStatus.CANCELLED)
                .note(note != null ? note : order.getNote())
                .build();

        final Order saved = orderRepository.save(updated);
        log.info("Cancelled order: {} by requester: {}", id, currentUserId);

        // Trigger audit logging
        auditService.logEvent(
                "ORDER",
                id,
                "CANCELLED",
                order.getStatus().name(),
                OrderStatus.CANCELLED.name(),
                note
        );

        // Send notification
        smsNotificationService.notifyOrderCancelled(saved);

        return saved;
    }

    /**
     * Rejects an order by handler (Admin for LEASE, VLE for RENT).
     *
     * <p>Business Decision:
     * - LEASE orders: Only ADMIN can reject (VLE is requester, not handler)
     * - RENT orders: Only VLE can reject (VLE is handler)
     * - Can reject from INTEREST_RAISED or UNDER_REVIEW status
     *
     * @param id order ID
     * @param note rejection note
     * @return rejected order
     */
    @Override
    @Transactional
    public Order rejectOrder(final String id, final String note) {
        final Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // BUSINESS DECISION: Validate order type and handler
        // LEASE orders: Handler is ADMIN (VLE is requester, cannot reject their own order)
        // RENT orders: Handler is VLE (VLE can reject)
        if (order.getOrderType() == OrderType.LEASE) {
            // For LEASE orders, only ADMIN can reject
            if (!userContext.hasRole(UserRole.ADMIN) && !userContext.hasRole(UserRole.SUPER_ADMIN)) {
                throw new RuntimeException("Only admins can reject LEASE orders. VLE is the requester, not the handler.");
            }
        } else if (order.getOrderType() == OrderType.RENT) {
            // For RENT orders, only the assigned VLE (handler) can reject
            validateHandlerAccess(order);
        }

        // BUSINESS DECISION: Can only reject from INTEREST_RAISED or UNDER_REVIEW
        if (order.getStatus() != OrderStatus.INTEREST_RAISED && order.getStatus() != OrderStatus.UNDER_REVIEW) {
            throw new RuntimeException("Order can only be rejected when status is INTEREST_RAISED or UNDER_REVIEW");
        }

        // Validate state transition
        if (!OrderStateMachine.canTransition(order.getStatus(), OrderStatus.REJECTED)) {
            throw new RuntimeException("Cannot reject order in current status");
        }

        // Update status to REJECTED
        final Order updated = order.toBuilder()
                .status(OrderStatus.REJECTED)
                .note(note != null ? note : order.getNote())
                .build();

        final Order saved = orderRepository.save(updated);
        log.info("Rejected order: {} by handler", id);

        // Trigger audit logging
        auditService.logEvent(
                "ORDER",
                id,
                "REJECTED",
                order.getStatus().name(),
                OrderStatus.REJECTED.name(),
                note
        );

        // Send notification
        smsNotificationService.notifyOrderRejected(saved);

        return saved;
    }

    /**
     * Gets all LEASE orders (for Admin).
     *
     * <p>Business Decision:
     * - Only users with ADMIN role can access
     *
     * @return list of LEASE orders
     */
    @Override
    public List<Order> getLeaseOrders() {
        // BUSINESS DECISION: Only admins can view LEASE orders
        if (!userContext.hasRole(UserRole.ADMIN) && !userContext.hasRole(UserRole.SUPER_ADMIN)) {
            throw new RuntimeException("Only admins can view LEASE orders");
        }

        return orderRepository.findAll().stream()
                .filter(order -> order.getOrderType() == OrderType.LEASE)
                .toList();
    }

    /**
     * Gets all RENT orders assigned to current VLE.
     *
     * <p>Business Decision:
     * - Only VLEs can access
     * - Returns orders where current VLE is the handler
     *
     * @return list of RENT orders
     */
    @Override
    public List<Order> getRentOrders() {
        final String currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        // BUSINESS DECISION: Get VLE profile to verify user is a VLE
        final VleProfile vleProfile = vleProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("VLE profile not found for user"));

        // Get all RENT orders where this VLE is the handler
        // Handler ID is the VLE profile ID
        return orderRepository.findAll().stream()
                .filter(order -> order.getOrderType() == OrderType.RENT)
                .filter(order -> {
                    final Handler handler = order.getHandledBy();
                    return handler != null
                            && handler.getType() == HandlerType.VLE
                            && handler.getId().equals(vleProfile.getId());
                })
                .toList();
    }
}

