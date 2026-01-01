package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.domain.enums.PricingMetric;
import com.kissanmitra.dto.PricingRuleItem;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.ThresholdConfig;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private ThresholdConfig thresholdConfig;
    private PricingRule defaultRule;
    private PricingRule timeSpecificRule;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        thresholdConfig = ThresholdConfig.builder()
                .deviceTypeId("device_type_tractor")
                .maxRentalHours(24)
                .maxRentalAcres(10)
                .status("ACTIVE")
                .build();

        defaultRule = PricingRule.builder()
                .id("rule-1")
                .deviceTypeId("device_type_tractor")
                .pincode("560001")
                .rules(List.of(
                        PricingRuleItem.builder().metric(PricingMetric.PER_HOUR).rate(500.0).build(),
                        PricingRuleItem.builder().metric(PricingMetric.PER_ACRE).rate(2000.0).build()
                ))
                .effectiveFrom(LocalDate.now().minusDays(30))
                .effectiveTo(null) // Default rule (ongoing)
                .status("ACTIVE")
                .build();

        timeSpecificRule = PricingRule.builder()
                .id("rule-2")
                .deviceTypeId("device_type_tractor")
                .pincode("560001")
                .rules(List.of(
                        PricingRuleItem.builder().metric(PricingMetric.PER_HOUR).rate(600.0).build()
                ))
                .effectiveFrom(LocalDate.now().minusDays(5))
                .effectiveTo(LocalDate.now().plusDays(5))
                .status("ACTIVE")
                .build();

        testDevice = Device.builder()
                .id("device-id")
                .deviceTypeId("device_type_tractor")
                .pincode("560001")
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

    @Test
    void testGetDefaultRule_Success() {
        // Given
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE")))
                .thenReturn(Optional.of(defaultRule));

        // When
        PricingRule result = pricingService.getDefaultRule("device_type_tractor", "560001");

        // Then
        assertNotNull(result);
        assertEquals("rule-1", result.getId());
        assertNull(result.getEffectiveTo()); // Default rule has null effectiveTo
    }

    @Test
    void testGetDefaultRule_NotFound() {
        // Given
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        PricingRule result = pricingService.getDefaultRule("device_type_tractor", "560001");

        // Then
        assertNull(result);
    }

    @Test
    void testGetTimeSpecificRules_Success() {
        // Given
        LocalDate today = LocalDate.now();
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE"), eq(today), eq(today)))
                .thenReturn(List.of(timeSpecificRule));

        // When
        List<PricingRule> result = pricingService.getTimeSpecificRules("device_type_tractor", "560001", today);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("rule-2", result.get(0).getId());
    }

    @Test
    void testHasActivePricingRule_True() {
        // Given
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE")))
                .thenReturn(Optional.of(defaultRule));

        // When
        boolean result = pricingService.hasActivePricingRule("device_type_tractor", "560001");

        // Then
        assertTrue(result);
    }

    @Test
    void testHasActivePricingRule_False() {
        // Given
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        boolean result = pricingService.hasActivePricingRule("device_type_tractor", "560001");

        // Then
        assertFalse(result);
    }

    @Test
    void testGetActivePricingForDevice_TimeSpecificTakesPrecedence() {
        // Given
        LocalDate today = LocalDate.now();
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE"), eq(today), eq(today)))
                .thenReturn(List.of(timeSpecificRule));

        // When
        PricingRule result = pricingService.getActivePricingForDevice("device-id", today);

        // Then
        assertNotNull(result);
        assertEquals("rule-2", result.getId()); // Time-specific rule takes precedence
    }

    @Test
    void testGetActivePricingForDevice_FallsBackToDefault() {
        // Given
        LocalDate today = LocalDate.now();
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList()); // No time-specific rule
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE")))
                .thenReturn(Optional.of(defaultRule));

        // When
        PricingRule result = pricingService.getActivePricingForDevice("device-id", today);

        // Then
        assertNotNull(result);
        assertEquals("rule-1", result.getId()); // Falls back to default rule
    }

    @Test
    void testGetActivePricingForDevice_NoRuleFound() {
        // Given
        LocalDate today = LocalDate.now();
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        PricingRule result = pricingService.getActivePricingForDevice("device-id", today);

        // Then
        assertNull(result);
    }

    @Test
    void testCheckForConflicts_DefaultRuleExists() {
        // Given
        PricingRule newDefaultRule = PricingRule.builder()
                .deviceTypeId("device_type_tractor")
                .pincode("560001")
                .effectiveTo(null)
                .build();

        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE")))
                .thenReturn(Optional.of(defaultRule));

        // When
        List<PricingRule> conflicts = pricingService.checkForConflicts(newDefaultRule);

        // Then
        assertEquals(1, conflicts.size());
        assertEquals("rule-1", conflicts.get(0).getId());
    }

    @Test
    void testCheckForConflicts_OverlappingTimeSpecificRules() {
        // Given
        PricingRule newTimeRule = PricingRule.builder()
                .deviceTypeId("device_type_tractor")
                .pincode("560001")
                .effectiveFrom(LocalDate.now().minusDays(3))
                .effectiveTo(LocalDate.now().plusDays(3))
                .build();

        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNotNullAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE"),
                eq(newTimeRule.getEffectiveTo()), eq(newTimeRule.getEffectiveFrom())))
                .thenReturn(List.of(timeSpecificRule));

        // When
        List<PricingRule> conflicts = pricingService.checkForConflicts(newTimeRule);

        // Then
        assertEquals(1, conflicts.size());
        assertEquals("rule-2", conflicts.get(0).getId());
    }

    @Test
    void testGetPricingForDevice() {
        // Given
        LocalDate today = LocalDate.now();
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
                eq("device_type_tractor"), eq("560001"), eq("ACTIVE")))
                .thenReturn(Optional.of(defaultRule));

        // When
        PricingRule result = pricingService.getPricingForDevice("device-id");

        // Then
        assertNotNull(result);
        assertEquals("rule-1", result.getId());
    }
}

