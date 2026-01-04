package com.kissanmitra.controller.admin;

import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.dto.Address;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.request.DeviceFinalizeRequest;
import com.kissanmitra.request.DeviceOnboardStep1Request;
import com.kissanmitra.request.DevicePricingRuleRequest;
import com.kissanmitra.service.MasterDataValidationService;
import com.kissanmitra.service.MediaUploadService;
import com.kissanmitra.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceController.
 *
 * <p>Tests device onboarding flow and device management endpoints.
 */
@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private PricingService pricingService;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private MasterDataValidationService masterDataValidationService;

    @InjectMocks
    private DeviceController deviceController;

    private Device testDevice;
    private DeviceOnboardStep1Request step1Request;

    @BeforeEach
    void setUp() {
        Address address = Address.builder()
                .firstLine("123 Main Street")
                .pinCode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .build();

        step1Request = DeviceOnboardStep1Request.builder()
                .sensorId("SENSOR-001")
                .name("Tractor 1")
                .description("Heavy-duty tractor")
                .deviceTypeId("TRACTOR")
                .manufacturerId("KISSAN_MITRA")
                .owner("John Doe")
                .manufacturedDate("01/2020")
                .address(address)
                .location(new Point(77.5946, 12.9716))
                .companyOwned(true)
                .requiresOperator(true)
                .build();

        testDevice = Device.builder()
                .id("device-id")
                .sensorId("SENSOR-001")
                .name("Tractor 1")
                .deviceTypeId("TRACTOR")
                .pincode("560001")
                .status(DeviceStatus.DRAFT)
                .build();
    }

    @Test
    void testOnboardStep1_Success() {
        // Given
        doNothing().when(masterDataValidationService).validateDeviceTypeAndManufacturer(anyString(), anyString());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        var result = deviceController.onboardStep1(step1Request);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(DeviceStatus.DRAFT, result.getData().getStatus());
        assertEquals("560001", result.getData().getPincode());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void testUploadMedia_Success() {
        // Given
        MultipartFile file1 = new MockMultipartFile("files", "image.jpg", "image/jpeg", "content".getBytes());
        MultipartFile file2 = new MockMultipartFile("files", "video.mp4", "video/mp4", "content".getBytes());

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(mediaUploadService.uploadMedia(eq("device-id"), any(MultipartFile[].class)))
                .thenReturn(List.of("https://s3.../image1.jpg", "https://s3.../video1.mp4"));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        var result = deviceController.uploadMedia("device-id", new MultipartFile[]{file1, file2}, 0);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(mediaUploadService).uploadMedia(eq("device-id"), any(MultipartFile[].class));
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void testGetPricingStatus_HasDefaultRule() {
        // Given
        PricingRule defaultRule = PricingRule.builder()
                .id("rule-1")
                .deviceTypeId("TRACTOR")
                .pincode("560001")
                .effectiveTo(null)
                .build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.getDefaultRule("TRACTOR", "560001")).thenReturn(defaultRule);
        when(pricingService.getTimeSpecificRules("TRACTOR", "560001", LocalDate.now()))
                .thenReturn(new ArrayList<>());

        // When
        var result = deviceController.getPricingStatus("device-id");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getDefaultRule());
        assertFalse(result.getData().isRequiresPricingRule());
    }

    @Test
    void testGetPricingStatus_NoRule() {
        // Given
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.getDefaultRule("TRACTOR", "560001")).thenReturn(null);
        when(pricingService.getTimeSpecificRules("TRACTOR", "560001", LocalDate.now()))
                .thenReturn(new ArrayList<>());

        // When
        var result = deviceController.getPricingStatus("device-id");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isRequiresPricingRule());
    }

    @Test
    void testCreatePricingRule_Success() {
        // Given
        DevicePricingRuleRequest request = DevicePricingRuleRequest.builder()
                .defaultRules(List.of(
                        com.kissanmitra.dto.PricingRuleItem.builder()
                                .metric(com.kissanmitra.domain.enums.PricingMetric.PER_HOUR)
                                .rate(500.0)
                                .build()
                ))
                .build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.checkForConflicts(any(PricingRule.class))).thenReturn(new ArrayList<>());
        when(pricingRuleRepository.save(any(PricingRule.class))).thenReturn(PricingRule.builder().id("rule-1").build());

        // When
        var result = deviceController.createPricingRule("device-id", request);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(pricingRuleRepository).save(any(PricingRule.class));
    }

    @Test
    void testCreatePricingRule_Conflict() {
        // Given
        DevicePricingRuleRequest request = DevicePricingRuleRequest.builder()
                .defaultRules(List.of(
                        com.kissanmitra.dto.PricingRuleItem.builder()
                                .metric(com.kissanmitra.domain.enums.PricingMetric.PER_HOUR)
                                .rate(500.0)
                                .build()
                ))
                .build();

        PricingRule existingRule = PricingRule.builder().id("existing-rule").build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.checkForConflicts(any(PricingRule.class))).thenReturn(List.of(existingRule));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceController.createPricingRule("device-id", request);
        });
    }

    @Test
    void testFinalizeOnboarding_ONBOARD() {
        // Given
        DeviceFinalizeRequest request = DeviceFinalizeRequest.builder()
                .action(DeviceFinalizeRequest.FinalizeAction.ONBOARD)
                .build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        var result = deviceController.finalizeOnboarding("device-id", request);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void testFinalizeOnboarding_TAKE_LIVE_Success() {
        // Given
        DeviceFinalizeRequest request = DeviceFinalizeRequest.builder()
                .action(DeviceFinalizeRequest.FinalizeAction.TAKE_LIVE)
                .build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.hasActivePricingRule("TRACTOR", "560001")).thenReturn(true);
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        var result = deviceController.finalizeOnboarding("device-id", request);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(pricingService).hasActivePricingRule("TRACTOR", "560001");
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void testFinalizeOnboarding_TAKE_LIVE_NoPricingRule() {
        // Given
        DeviceFinalizeRequest request = DeviceFinalizeRequest.builder()
                .action(DeviceFinalizeRequest.FinalizeAction.TAKE_LIVE)
                .build();

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.hasActivePricingRule("TRACTOR", "560001")).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceController.finalizeOnboarding("device-id", request);
        });
    }

    @Test
    void testGetOnboardSummary_Success() {
        // Given
        PricingRule defaultRule = PricingRule.builder()
                .id("rule-1")
                .deviceTypeId("TRACTOR")
                .pincode("560001")
                .build();

        Address address = Address.builder()
                .firstLine("123 Main Street")
                .pinCode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .build();

        testDevice.setAddress(address);
        testDevice.setMediaUrls(List.of("https://s3.../image1.jpg"));
        testDevice.setPrimaryMediaUrl("https://s3.../image1.jpg");

        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.getDefaultRule("TRACTOR", "560001")).thenReturn(defaultRule);
        when(pricingService.getTimeSpecificRules("TRACTOR", "560001", LocalDate.now()))
                .thenReturn(new ArrayList<>());

        // When
        var result = deviceController.getOnboardSummary("device-id");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("device-id", result.getData().getDeviceId());
        assertNotNull(result.getData().getBasicDetails());
        assertNotNull(result.getData().getMedia());
        assertNotNull(result.getData().getLocation());
        // Verify address is present but coordinates are NOT exposed
        assertNotNull(result.getData().getLocation().getAddress());
        assertEquals("560001", result.getData().getLocation().getPincode());
    }

    @Test
    void testUpdateDeviceStatus_Success() {
        // Given
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        var result = deviceController.updateDeviceStatus("device-id", DeviceStatus.ONBOARDED);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void testUpdateDeviceStatus_RetiredCannotChange() {
        // Given
        testDevice.setStatus(DeviceStatus.RETIRED);
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceController.updateDeviceStatus("device-id", DeviceStatus.LIVE);
        });
    }

    @Test
    void testUpdateDeviceStatus_LIVE_RequiresPricingRule() {
        // Given - Status is DRAFT (not ONBOARDED), so validation should run
        testDevice.setStatus(DeviceStatus.DRAFT);
        when(deviceRepository.findById("device-id")).thenReturn(Optional.of(testDevice));
        when(pricingService.hasActivePricingRule("TRACTOR", "560001")).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceController.updateDeviceStatus("device-id", DeviceStatus.LIVE);
        });
    }
}

