package com.kissanmitra.service;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.entity.Order;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.request.DiscoverySearchRequest;
import com.kissanmitra.response.DiscoveryResponse;
import com.kissanmitra.service.impl.DiscoveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DiscoveryService.
 *
 * <p>Tests geospatial search and intent creation.
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private DiscoveryIntentRepository discoveryIntentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    private Device testDevice;
    private DiscoverySearchRequest searchRequest;

    @BeforeEach
    void setUp() {
        testDevice = Device.builder()
                .id("device-id")
                .deviceTypeId("device-type-id")
                .pincode("560001")
                .status(DeviceStatus.LIVE)
                .location(new Point(77.5946, 12.9716))
                .build();

        DiscoverySearchRequest.LocationInput locationInput = DiscoverySearchRequest.LocationInput.builder()
                .lat(12.9716)
                .lng(77.5946)
                .build();

        DiscoverySearchRequest.IntentInput intentInput = DiscoverySearchRequest.IntentInput.builder()
                .requestedHours(5.0)
                .requestedAcres(2.0)
                .build();

        searchRequest = DiscoverySearchRequest.builder()
                .location(locationInput)
                .intent(intentInput)
                .build();

        // Default: unauthenticated user (null userId and activeRole)
        // Use lenient() since not all tests use these mocks
        lenient().when(userContext.getCurrentUserId()).thenReturn(null);
        lenient().when(userContext.getCurrentUserActiveRole()).thenReturn(null);
    }

    @Test
    void testSearchDevices_Success() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);
        when(pricingService.getActivePricingForDevice(anyString(), any())).thenReturn(null);
        lenient().when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of()); // No unavailable orders

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
        assertEquals("device-id", result.getResults().get(0).getDeviceId());
        assertEquals("RENT", result.getResults().get(0).getIntentType());
    }

    @Test
    void testSearchDevices_FiltersNonLiveDevices() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice)); // Repository already filters by LIVE status

        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);
        when(pricingService.getActivePricingForDevice(anyString(), any())).thenReturn(null);
        lenient().when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of()); // No unavailable orders

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(1, result.getResults().size()); // Only LIVE device included
        assertEquals("device-id", result.getResults().get(0).getDeviceId());
    }

    @Test
    void testSearchDevices_FiltersDevicesWithoutPricingRules() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));

        // Device has no pricing rule
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(false);

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(0, result.getResults().size()); // Device excluded due to no pricing rule
    }

    @Test
    void testSearchDevices_FiltersDevicesWithAcceptedOrder() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        
        Order acceptedOrder = Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .status(OrderStatus.ACCEPTED)
                .build();
        when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of(acceptedOrder));

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(0, result.getResults().size()); // Device excluded due to ACCEPTED order
    }

    @Test
    void testSearchDevices_FiltersDevicesWithPickupScheduledOrder() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        
        Order scheduledOrder = Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .status(OrderStatus.PICKUP_SCHEDULED)
                .build();
        when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of(scheduledOrder));

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(0, result.getResults().size()); // Device excluded due to PICKUP_SCHEDULED order
    }

    @Test
    void testSearchDevices_FiltersDevicesWithActiveOrder() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        
        Order activeOrder = Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .status(OrderStatus.ACTIVE)
                .build();
        when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of(activeOrder));

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(0, result.getResults().size()); // Device excluded due to ACTIVE order
    }

    @Test
    void testSearchDevices_FiltersDevicesWithCompletedOrder() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        
        Order completedOrder = Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .status(OrderStatus.COMPLETED)
                .build();
        when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of(completedOrder));

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(0, result.getResults().size()); // Device excluded due to COMPLETED order
    }

    @Test
    void testSearchDevices_AllowsDevicesWithClosedOrder() {
        // Given - CLOSED orders should allow device to be discoverable
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);
        when(pricingService.getActivePricingForDevice(anyString(), any())).thenReturn(null);
        
        Order closedOrder = Order.builder()
                .id("order-id")
                .deviceId("device-id")
                .status(OrderStatus.CLOSED)
                .build();
        lenient().when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of(closedOrder));

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(1, result.getResults().size()); // Device included despite CLOSED order
        assertEquals("device-id", result.getResults().get(0).getDeviceId());
    }

    @Test
    void testSearchDevices_NoResults() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(Collections.emptyList());

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void testSearchDevices_Pagination() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.LIVE)))
                .thenReturn(List.of(testDevice));
        when(pricingService.hasActivePricingRule(anyString(), anyString())).thenReturn(true);
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);
        when(pricingService.getActivePricingForDevice(anyString(), any())).thenReturn(null);
        lenient().when(orderRepository.findByDeviceId("device-id")).thenReturn(List.of()); // No unavailable orders

        DiscoverySearchRequest.SearchFilters filters = DiscoverySearchRequest.SearchFilters.builder()
                .page(0)
                .pageSize(10)
                .build();
        searchRequest.setFilters(filters);

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertNotNull(result.getPage());
        assertNotNull(result.getPageSize());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void testCreateIntent_Success() {
        // Given
        DiscoveryIntent savedIntent = DiscoveryIntent.builder()
                .id("intent-id")
                .deviceId("device-id")
                .intentType("RENT")
                .build();

        when(discoveryIntentRepository.save(any(DiscoveryIntent.class))).thenReturn(savedIntent);

        // When
        String intentId = discoveryService.createIntent("device-id", "RENT", 5.0, 2.0);

        // Then
        assertNotNull(intentId);
        verify(discoveryIntentRepository).save(any(DiscoveryIntent.class));
    }
}

