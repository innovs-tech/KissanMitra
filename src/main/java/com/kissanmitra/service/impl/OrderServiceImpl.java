package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.HandlerType;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.request.CreateOrderRequest;
import com.kissanmitra.request.UpdateOrderStatusRequest;
import com.kissanmitra.service.AuditService;
import com.kissanmitra.service.OrderService;
import com.kissanmitra.service.OrderStateMachine;
import com.kissanmitra.service.PricingService;
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
 * - Order type is automatically derived from thresholds
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
    private final PricingService pricingService;
    private final UserContext userContext;
    private final AuditService auditService;

    /**
     * Creates a new order.
     *
     * <p>Business Decision:
     * - Order type derived from thresholds
     * - Handler set based on order type (ADMIN for LEASE, VLE for RENT)
     * - Status set to INTEREST_RAISED
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

        // Derive order type from thresholds
        final OrderType orderType = pricingService.deriveOrderType(
                device.getDeviceTypeId(),
                request.getRequestedHours(),
                request.getRequestedAcres()
        );

        // Determine handler based on order type
        final Handler handler = determineHandler(orderType);

        // Create order
        final Order order = Order.builder()
                .orderType(orderType)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(request.getDeviceId())
                .requestedBy(userContext.getCurrentUserId())
                .handledBy(handler)
                .requestedHours(request.getRequestedHours())
                .requestedAcres(request.getRequestedAcres())
                .note(request.getNote())
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

        log.info("Created order: {} of type: {} for device: {}", saved.getId(), orderType, request.getDeviceId());
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

        // TODO: Send notifications

        return saved;
    }

    private Handler determineHandler(final OrderType orderType) {
        if (orderType == OrderType.LEASE) {
            // BUSINESS DECISION: LEASE orders handled by Admin
            return Handler.builder()
                    .type(HandlerType.ADMIN)
                    .id("admin") // TODO: Get actual admin ID from context
                    .build();
        } else {
            // BUSINESS DECISION: RENT orders handled by VLE (device owner)
            // For now, return placeholder - should get VLE from device's current lease
            return Handler.builder()
                    .type(HandlerType.VLE)
                    .id("vle") // TODO: Get actual VLE ID from device's current lease
                    .build();
        }
    }

    private void validateHandlerAccess(final Order order) {
        final String currentUserId = userContext.getCurrentUserId();
        final Handler handler = order.getHandledBy();

        if (handler == null) {
            throw new RuntimeException("Order handler not set");
        }

        // TODO: Implement proper handler validation
        // For ADMIN: Check if user has ADMIN role
        // For VLE: Check if user is the VLE
    }
}

