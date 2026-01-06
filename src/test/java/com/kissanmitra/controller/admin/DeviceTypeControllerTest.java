package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DeviceTypeRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.request.CreateDeviceTypeRequest;
import com.kissanmitra.request.UpdateDeviceTypeRequest;
import com.kissanmitra.service.MasterDataService;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceTypeController.
 *
 * <p>Tests CRUD operations, code immutability, and soft delete validation.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTypeControllerTest {

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private ThresholdConfigRepository thresholdConfigRepository;

    @Mock
    private MasterDataService masterDataService;

    @InjectMocks
    private DeviceTypeController deviceTypeController;

    private DeviceType testDeviceType;
    private CreateDeviceTypeRequest createRequest;
    private UpdateDeviceTypeRequest updateRequest;

    @BeforeEach
    void setUp() {
        testDeviceType = DeviceType.builder()
                .code("TRACTOR")
                .displayName("Heavy Duty Tractor")
                .requiresOperator(true)
                .active(true)
                .build();
        testDeviceType.setId("type-id");

        createRequest = CreateDeviceTypeRequest.builder()
                .code("HARVESTER")
                .displayName("Combine Harvester")
                .requiresOperator(true)
                .active(true)
                .build();

        updateRequest = UpdateDeviceTypeRequest.builder()
                .displayName("Updated Tractor Name")
                .requiresOperator(false)
                .active(true)
                .build();
    }

    @Test
    void testCreateDeviceType_Success() {
        // Given
        when(deviceTypeRepository.findByCode("HARVESTER")).thenReturn(Optional.empty());
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(testDeviceType);

        // When
        var result = deviceTypeController.createDeviceType(createRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceTypeRepository).findByCode("HARVESTER");
        verify(deviceTypeRepository).save(any(DeviceType.class));
    }

    @Test
    void testCreateDeviceType_DuplicateCode() {
        // Given
        when(deviceTypeRepository.findByCode("HARVESTER")).thenReturn(Optional.of(testDeviceType));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceTypeController.createDeviceType(createRequest);
        });
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }

    @Test
    void testGetAllDeviceTypes_WithActiveFilter() {
        // Given
        when(deviceTypeRepository.findByActive(true)).thenReturn(List.of(testDeviceType));

        // When
        var result = deviceTypeController.getAllDeviceTypes(true);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        verify(deviceTypeRepository).findByActive(true);
    }

    @Test
    void testGetAllDeviceTypes_WithoutFilter() {
        // Given
        when(deviceTypeRepository.findAll()).thenReturn(List.of(testDeviceType));

        // When
        var result = deviceTypeController.getAllDeviceTypes(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceTypeRepository).findAll();
    }

    @Test
    void testGetDeviceTypeByCode_Success() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));

        // When
        var result = deviceTypeController.getDeviceTypeByCode("TRACTOR");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("TRACTOR", result.getData().getCode());
    }

    @Test
    void testGetDeviceTypeByCode_NotFound() {
        // Given
        when(deviceTypeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceTypeController.getDeviceTypeByCode("INVALID");
        });
    }

    @Test
    void testUpdateDeviceType_Success() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(testDeviceType);

        // When
        var result = deviceTypeController.updateDeviceType("TRACTOR", updateRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceTypeRepository).save(any(DeviceType.class));
    }

    @Test
    void testUpdateDeviceType_CodeImmutability() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(testDeviceType);

        // When - Code should not be changed even if request tries to
        var result = deviceTypeController.updateDeviceType("TRACTOR", updateRequest);

        // Then - Code remains unchanged
        assertNotNull(result);
        verify(deviceTypeRepository).save(argThat(deviceType ->
                "TRACTOR".equals(deviceType.getCode()) // Code unchanged
        ));
    }

    @Test
    void testDeleteDeviceType_Success() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(pricingRuleRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(thresholdConfigRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(testDeviceType);

        // When
        var result = deviceTypeController.deleteDeviceType("TRACTOR");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deviceTypeRepository).save(argThat(deviceType ->
                Boolean.FALSE.equals(deviceType.getActive()) // Soft delete
        ));
    }

    @Test
    void testDeleteDeviceType_HasDeviceReferences() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceRepository.countByDeviceTypeId("TRACTOR")).thenReturn(5L);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceTypeController.deleteDeviceType("TRACTOR");
        });
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }

    @Test
    void testDeleteDeviceType_HasPricingRuleReferences() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(pricingRuleRepository.countByDeviceTypeId("TRACTOR")).thenReturn(3L);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceTypeController.deleteDeviceType("TRACTOR");
        });
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }

    @Test
    void testDeleteDeviceType_HasThresholdConfigReferences() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));
        when(deviceRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(pricingRuleRepository.countByDeviceTypeId("TRACTOR")).thenReturn(0L);
        when(thresholdConfigRepository.countByDeviceTypeId("TRACTOR")).thenReturn(2L);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceTypeController.deleteDeviceType("TRACTOR");
        });
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }
}

