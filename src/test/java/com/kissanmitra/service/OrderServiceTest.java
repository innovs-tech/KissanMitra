package com.kissanmitra.service;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.domain.enums.HandlerType;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import com.kissanmitra.entity.Device;
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
import com.kissanmitra.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 *
 * <p>Tests order creation, status updates, and state machine validation.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private DiscoveryIntentRepository discoveryIntentRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private VleProfileRepository vleProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private UserContext userContext;

    @Mock
    private com.kissanmitra.service.SmsNotificationService smsNotificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final String TEST_DEVICE_ID = "device-id";
    private static final String TEST_USER_ID = "user-id";
    private static final String TEST_ORDER_ID = "order-id";

    private Device testDevice;
    private CreateOrderRequest createRequest;

    @BeforeEach
    void setUp() {
        testDevice = Device.builder()
                .id(TEST_DEVICE_ID)
                .deviceTypeId("device-type-id")
                .status(DeviceStatus.LIVE)
                .build();

        createRequest = CreateOrderRequest.builder()
                .deviceId(TEST_DEVICE_ID)
                .requestedHours(5.0)
                .requestedAcres(2.0)
                .build();
    }

    @Test
    void testCreateOrder_Success() {
        // Given - For RENT orders, FARMER is requester, device must have a lease
        testDevice.setCurrentLeaseId("lease-id");
        final String vleProfileId = "vle-profile-id";
        
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(userContext.getCurrentUserActiveRole()).thenReturn(UserRole.FARMER);
        when(userContext.getCurrentUserPhone()).thenReturn("+919876543210");
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));
        
        User user = User.builder()
                .id(TEST_USER_ID)
                .phone("+919876543210")
                .build();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        
        Lease lease = Lease.builder()
                .id("lease-id")
                .vleId(vleProfileId)
                .build();
        when(leaseRepository.findById("lease-id")).thenReturn(Optional.of(lease));
        
        Order savedOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .orderType(OrderType.RENT)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(TEST_DEVICE_ID)
                .requestedBy(TEST_USER_ID)
                .handledBy(Handler.builder()
                        .type(HandlerType.VLE)
                        .id(vleProfileId)
                        .build())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(smsNotificationService).notifyOrderCreated(any(Order.class));

        // When
        Order result = orderService.createOrder(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderType.RENT, result.getOrderType());
        assertEquals(OrderStatus.INTEREST_RAISED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(smsNotificationService).notifyOrderCreated(any(Order.class));
    }

    @Test
    void testCreateOrder_LeaseOrder_Success() {
        // Given - For LEASE orders, VLE is requester, Admin is handler, device must NOT be leased
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(userContext.getCurrentUserActiveRole()).thenReturn(UserRole.VLE);
        when(userContext.getCurrentUserPhone()).thenReturn("+919876543210");
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));
        // Device has no currentLeaseId (null)
        
        User user = User.builder()
                .id(TEST_USER_ID)
                .phone("+919876543210")
                .build();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        
        Order savedOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .orderType(OrderType.LEASE)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(TEST_DEVICE_ID)
                .requestedBy(TEST_USER_ID)
                .handledBy(Handler.builder()
                        .type(HandlerType.ADMIN)
                        .id("admin")
                        .build())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(smsNotificationService).notifyOrderCreated(any(Order.class));

        // When
        Order result = orderService.createOrder(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderType.LEASE, result.getOrderType());
        assertEquals(OrderStatus.INTEREST_RAISED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(smsNotificationService).notifyOrderCreated(any(Order.class));
    }

    @Test
    void testCreateOrder_DeviceNotFound() {
        // Given
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createRequest);
        });
    }

    @Test
    void testCreateOrder_DeviceNotAvailable() {
        // Given
        testDevice.setStatus(DeviceStatus.NOT_LIVE);
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createRequest);
        });
    }


    @Test
    void testCreateOrder_DeviceHasClosedOrder_ShouldAllow() {
        // Given - CLOSED orders should allow new orders
        testDevice.setCurrentLeaseId("lease-id");
        final String vleProfileId = "vle-profile-id";
        
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(userContext.getCurrentUserActiveRole()).thenReturn(UserRole.FARMER);
        when(userContext.getCurrentUserPhone()).thenReturn("+919876543210");
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));
        
        User user = User.builder()
                .id(TEST_USER_ID)
                .phone("+919876543210")
                .build();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        
        Lease lease = Lease.builder()
                .id("lease-id")
                .vleId(vleProfileId)
                .build();
        when(leaseRepository.findById("lease-id")).thenReturn(Optional.of(lease));
        
        Order savedOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .orderType(OrderType.RENT)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(TEST_DEVICE_ID)
                .requestedBy(TEST_USER_ID)
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(smsNotificationService).notifyOrderCreated(any(Order.class));

        // When
        Order result = orderService.createOrder(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.INTEREST_RAISED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(smsNotificationService).notifyOrderCreated(any(Order.class));
    }

    @Test
    void testGetOrderById_Success() {
        // Given
        Order order = Order.builder()
                .id(TEST_ORDER_ID)
                .status(OrderStatus.INTEREST_RAISED)
                .build();

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(order));

        // When
        Order result = orderService.getOrderById(TEST_ORDER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getId());
    }

    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(TEST_ORDER_ID);
        });
    }

    @Test
    void testGetMyOrders_Success() {
        // Given
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
        Order order1 = Order.builder().id("order-1").requestedBy(TEST_USER_ID).build();
        Order order2 = Order.builder().id("order-2").requestedBy(TEST_USER_ID).build();
        Order otherOrder = Order.builder().id("order-3").requestedBy("other-user").build();

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2, otherOrder));

        // When
        List<Order> result = orderService.getMyOrders();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getRequestedBy().equals(TEST_USER_ID)));
    }

    @Test
    void testUpdateOrderStatus_ValidTransition() {
        // Given - For RENT orders, VLE is the handler
        final String vleProfileId = "vle-profile-id";
        final String vleUserId = "vle-user-id";
        
        Order existingOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .status(OrderStatus.INTEREST_RAISED)
                .orderType(OrderType.RENT)
                .requestedBy(TEST_USER_ID)
                .handledBy(Handler.builder()
                        .type(HandlerType.VLE)
                        .id(vleProfileId)
                        .build())
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .toState(OrderStatus.UNDER_REVIEW)
                .note("Reviewing")
                .build();

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(userContext.getCurrentUserId()).thenReturn(vleUserId);
        
        VleProfile vleProfile = VleProfile.builder()
                .id(vleProfileId)
                .userId(vleUserId)
                .build();
        when(vleProfileRepository.findByUserId(vleUserId)).thenReturn(Optional.of(vleProfile));
        
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);
        doNothing().when(smsNotificationService).notifyOrderStatusUpdated(any(Order.class), any(OrderStatus.class));

        // When
        Order result = orderService.updateOrderStatus(TEST_ORDER_ID, request);

        // Then
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
        verify(auditService).logEvent(eq("ORDER"), eq(TEST_ORDER_ID), anyString(), eq("INTEREST_RAISED"), eq("UNDER_REVIEW"), eq("Reviewing"));
        verify(smsNotificationService).notifyOrderStatusUpdated(any(Order.class), eq(OrderStatus.INTEREST_RAISED));
    }

    @Test
    void testUpdateOrderStatus_InvalidTransition() {
        // Given
        Order existingOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .status(OrderStatus.INTEREST_RAISED)
                .handledBy(Handler.builder()
                        .type(HandlerType.VLE)
                        .id("vle")
                        .build())
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .toState(OrderStatus.ACTIVE)
                .build();

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(existingOrder));
        // OrderStateMachine.canTransition will return false for INTEREST_RAISED -> ACTIVE

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(TEST_ORDER_ID, request);
        });
    }

    @Test
    void testUpdateOrderStatus_OrderNotFound() {
        // Given
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .toState(OrderStatus.UNDER_REVIEW)
                .build();

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(TEST_ORDER_ID, request);
        });
    }
}

