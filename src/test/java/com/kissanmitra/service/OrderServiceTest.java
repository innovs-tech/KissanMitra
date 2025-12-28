package com.kissanmitra.service;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.domain.enums.HandlerType;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.Order;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.repository.OrderRepository;
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
    private AuditService auditService;

    @Mock
    private UserContext userContext;

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
                .status(DeviceStatus.WORKING)
                .build();

        createRequest = CreateOrderRequest.builder()
                .deviceId(TEST_DEVICE_ID)
                .requestedHours(5.0)
                .requestedAcres(2.0)
                .build();
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);
        
        Order savedOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .orderType(OrderType.RENT)
                .status(OrderStatus.INTEREST_RAISED)
                .deviceId(TEST_DEVICE_ID)
                .requestedBy(TEST_USER_ID)
                .handledBy(Handler.builder()
                        .type(HandlerType.VLE)
                        .id("vle")
                        .build())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order result = orderService.createOrder(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderType.RENT, result.getOrderType());
        assertEquals(OrderStatus.INTEREST_RAISED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
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
        testDevice.setStatus(DeviceStatus.NOT_WORKING);
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createRequest);
        });
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
        // Given
        Order existingOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .status(OrderStatus.INTEREST_RAISED)
                .requestedBy(TEST_USER_ID)
                .handledBy(Handler.builder()
                        .type(HandlerType.VLE)
                        .id("vle")
                        .build())
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .toState(OrderStatus.UNDER_REVIEW)
                .note("Reviewing")
                .build();

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        // When
        Order result = orderService.updateOrderStatus(TEST_ORDER_ID, request);

        // Then
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
        verify(auditService).logEvent(eq("ORDER"), eq(TEST_ORDER_ID), anyString(), eq("INTEREST_RAISED"), eq("UNDER_REVIEW"), eq("Reviewing"));
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

