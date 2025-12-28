package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.ThresholdConfig;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PricingService.
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private ThresholdConfigRepository thresholdConfigRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private ThresholdConfig thresholdConfig;

    @BeforeEach
    void setUp() {
        thresholdConfig = ThresholdConfig.builder()
                .deviceTypeId("device_type_tractor")
                .maxRentalHours(24)
                .maxRentalAcres(10)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testDeriveOrderType_Rent() {
        // Within rental thresholds -> RENT
        when(thresholdConfigRepository.findByDeviceTypeIdAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.of(thresholdConfig));

        final OrderType result = pricingService.deriveOrderType("device_type_tractor", 20.0, 8.0);
        assertEquals(OrderType.RENT, result);
    }

    @Test
    void testDeriveOrderType_Lease() {
        // Exceeds rental thresholds -> LEASE
        when(thresholdConfigRepository.findByDeviceTypeIdAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.of(thresholdConfig));

        final OrderType result = pricingService.deriveOrderType("device_type_tractor", 30.0, 12.0);
        assertEquals(OrderType.LEASE, result);
    }

    @Test
    void testDeriveOrderType_Lease_ExceedsHours() {
        // Exceeds hours threshold -> LEASE
        when(thresholdConfigRepository.findByDeviceTypeIdAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.of(thresholdConfig));

        final OrderType result = pricingService.deriveOrderType("device_type_tractor", 25.0, 8.0);
        assertEquals(OrderType.LEASE, result);
    }

    @Test
    void testDeriveOrderType_Lease_ExceedsAcres() {
        // Exceeds acres threshold -> LEASE
        when(thresholdConfigRepository.findByDeviceTypeIdAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.of(thresholdConfig));

        final OrderType result = pricingService.deriveOrderType("device_type_tractor", 20.0, 15.0);
        assertEquals(OrderType.LEASE, result);
    }

    @Test
    void testDeriveOrderType_ThresholdNotFound() {
        when(thresholdConfigRepository.findByDeviceTypeIdAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            pricingService.deriveOrderType("device_type_tractor", 20.0, 8.0);
        });
    }
}

