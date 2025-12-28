package com.kissanmitra.service;

import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
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
import org.springframework.data.geo.Metrics;
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

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    private Device testDevice;
    private DiscoverySearchRequest searchRequest;

    @BeforeEach
    void setUp() {
        testDevice = Device.builder()
                .id("device-id")
                .deviceTypeId("device-type-id")
                .status(DeviceStatus.WORKING)
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
    }

    @Test
    void testSearchDevices_Success() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.WORKING)))
                .thenReturn(List.of(testDevice));
        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);

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
    void testSearchDevices_FiltersNonWorkingDevices() {
        // Given
        Device nonWorkingDevice = Device.builder()
                .id("device-2")
                .status(DeviceStatus.NOT_WORKING)
                .build();

        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.WORKING)))
                .thenReturn(List.of(testDevice)); // Repository already filters by WORKING status

        when(pricingService.deriveOrderType(anyString(), any(), any())).thenReturn(OrderType.RENT);

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertEquals(1, result.getResults().size()); // Only WORKING device included
        assertEquals("device-id", result.getResults().get(0).getDeviceId());
    }

    @Test
    void testSearchDevices_NoResults() {
        // Given
        when(deviceRepository.findByLocationNearAndStatus(any(Point.class), any(Distance.class), eq(DeviceStatus.WORKING)))
                .thenReturn(Collections.emptyList());

        // When
        DiscoveryResponse result = discoveryService.searchDevices(searchRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getResults().isEmpty());
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

