package com.kissanmitra.service;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.OperatorAssignment;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.LeaseRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.request.CreateLeaseRequest;
import com.kissanmitra.service.impl.LeaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeaseService.
 *
 * <p>Tests lease creation, status updates, and operator assignment.
 */
@ExtendWith(MockitoExtension.class)
class LeaseServiceImplTest {

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private VleProfileRepository vleProfileRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private DocumentUploadService documentUploadService;

    @Mock
    private UserContext userContext;

    @Mock
    private com.kissanmitra.service.SmsNotificationService smsNotificationService;

    @InjectMocks
    private LeaseServiceImpl leaseService;

    private static final String TEST_ORDER_ID = "order-id";
    private static final String TEST_DEVICE_ID = "device-id";
    private static final String TEST_VLE_ID = "vle-id";
    private static final String TEST_USER_ID = "user-id";
    private static final String TEST_LEASE_ID = "lease-id";
    private static final String TEST_ADMIN_ID = "admin-id";

    private Order testOrder;
    private Device testDevice;
    private VleProfile testVleProfile;
    private CreateLeaseRequest createRequest;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(TEST_ORDER_ID)
                .deviceId(TEST_DEVICE_ID)
                .orderType(OrderType.LEASE)
                .status(OrderStatus.ACCEPTED)
                .requestedBy(TEST_USER_ID)
                .requestedHours(10.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .build();

        testDevice = Device.builder()
                .id(TEST_DEVICE_ID)
                .currentLeaseId(null)
                .build();

        testVleProfile = VleProfile.builder()
                .id(TEST_VLE_ID)
                .userId(TEST_USER_ID)
                .build();

        createRequest = CreateLeaseRequest.builder()
                .orderId(TEST_ORDER_ID)
                .depositAmount(1000.0)
                .build();

        // Use lenient to avoid UnnecessaryStubbingException in tests that don't use this
        lenient().when(userContext.getCurrentUserId()).thenReturn(TEST_ADMIN_ID);
    }

    @Test
    void testCreateLeaseFromOrder_Success() {
        // Given
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(testDevice));
        when(vleProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(testVleProfile));
        when(pricingService.getActivePricingForDevice(anyString(), any(LocalDate.class))).thenReturn(null);
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
            Lease lease = invocation.getArgument(0);
            lease.setId(TEST_LEASE_ID);
            return lease;
        });
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smsNotificationService).notifyLeaseCreated(any(Lease.class));

        // When
        final Lease result = leaseService.createLeaseFromOrder(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(TEST_LEASE_ID, result.getId());
        assertEquals(TEST_VLE_ID, result.getVleId());
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(LeaseStatus.ACTIVE, result.getStatus());
        verify(deviceRepository, times(1)).save(any(Device.class));
        verify(smsNotificationService, times(1)).notifyLeaseCreated(any(Lease.class));
    }

    @Test
    void testCreateLeaseFromOrder_OrderNotFound() {
        // Given
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            leaseService.createLeaseFromOrder(createRequest);
        });
    }

    @Test
    void testCreateLeaseFromOrder_NotLeaseOrder() {
        // Given
        final Order rentOrder = testOrder.toBuilder()
                .orderType(OrderType.RENT)
                .build();
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(rentOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            leaseService.createLeaseFromOrder(createRequest);
        });
    }

    @Test
    void testCreateLeaseFromOrder_OrderNotAccepted() {
        // Given
        final Order pendingOrder = testOrder.toBuilder()
                .status(OrderStatus.INTEREST_RAISED)
                .build();
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(pendingOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            leaseService.createLeaseFromOrder(createRequest);
        });
    }

    @Test
    void testCreateLeaseFromOrder_DeviceAlreadyLeased() {
        // Given
        final Device leasedDevice = testDevice.toBuilder()
                .currentLeaseId("existing-lease-id")
                .build();
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(leasedDevice));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            leaseService.createLeaseFromOrder(createRequest);
        });
    }

    @Test
    void testGetLeaseById_Success() {
        // Given
        final Lease lease = Lease.builder()
                .id(TEST_LEASE_ID)
                .deviceId(TEST_DEVICE_ID)
                .vleId(TEST_VLE_ID)
                .status(LeaseStatus.ACTIVE)
                .build();

        when(leaseRepository.findById(TEST_LEASE_ID)).thenReturn(Optional.of(lease));

        // When
        final Lease result = leaseService.getLeaseById(TEST_LEASE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_LEASE_ID, result.getId());
    }

    @Test
    void testGetLeaseById_NotFound() {
        // Given
        when(leaseRepository.findById(TEST_LEASE_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            leaseService.getLeaseById(TEST_LEASE_ID);
        });
    }

    @Test
    void testGetLeasesByVleId() {
        // Given
        final List<Lease> leases = List.of(
                Lease.builder().id("lease-1").vleId(TEST_VLE_ID).build(),
                Lease.builder().id("lease-2").vleId(TEST_VLE_ID).build()
        );
        when(leaseRepository.findByVleIdAndStatus(eq(TEST_VLE_ID), any(LeaseStatus.class))).thenReturn(leases);

        // When
        final List<Lease> result = leaseService.getLeasesByVleId(TEST_VLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(leaseRepository, times(1)).findByVleIdAndStatus(eq(TEST_VLE_ID), eq(LeaseStatus.ACTIVE));
    }


    @Test
    void testAssignOperator_Success() {
        // Given
        final Lease lease = Lease.builder()
                .id(TEST_LEASE_ID)
                .operators(List.of())
                .build();

        final OperatorAssignment assignment = OperatorAssignment.builder()
                .operatorId("operator-id")
                .role(com.kissanmitra.domain.enums.OperatorRole.PRIMARY)
                .build();

        when(leaseRepository.findById(TEST_LEASE_ID)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smsNotificationService).notifyOperatorAssigned(any(Lease.class), anyString());

        // When
        final Lease result = leaseService.assignOperator(TEST_LEASE_ID, assignment);

        // Then
        assertNotNull(result);
        verify(smsNotificationService, times(1)).notifyOperatorAssigned(any(Lease.class), eq("operator-id"));
    }
}

