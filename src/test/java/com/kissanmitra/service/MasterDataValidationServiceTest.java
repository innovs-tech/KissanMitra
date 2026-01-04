package com.kissanmitra.service;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.service.impl.MasterDataValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MasterDataValidationService.
 *
 * <p>Tests validation logic for device types and manufacturers.
 */
@ExtendWith(MockitoExtension.class)
class MasterDataValidationServiceTest {

    @Mock
    private MasterDataService masterDataService;

    @InjectMocks
    private MasterDataValidationServiceImpl validationService;

    private DeviceType activeDeviceType;
    private DeviceType inactiveDeviceType;
    private Manufacturer activeManufacturer;
    private Manufacturer inactiveManufacturer;

    @BeforeEach
    void setUp() {
        activeDeviceType = DeviceType.builder()
                .code("TRACTOR")
                .displayName("Tractor")
                .active(true)
                .build();

        inactiveDeviceType = DeviceType.builder()
                .code("OLD_TYPE")
                .displayName("Old Type")
                .active(false)
                .build();

        activeManufacturer = Manufacturer.builder()
                .code("MAHINDRA")
                .name("Mahindra")
                .active(true)
                .build();

        inactiveManufacturer = Manufacturer.builder()
                .code("OLD_MFG")
                .name("Old Manufacturer")
                .active(false)
                .build();
    }

    @Test
    void testValidateDeviceType_Success() {
        // Given
        when(masterDataService.getDeviceTypeByCode("TRACTOR"))
                .thenReturn(Optional.of(activeDeviceType));

        // When & Then - Should not throw
        assertDoesNotThrow(() -> {
            validationService.validateDeviceType("TRACTOR");
        });
    }

    @Test
    void testValidateDeviceType_NotFound() {
        // Given
        when(masterDataService.getDeviceTypeByCode("INVALID"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateDeviceType("INVALID");
        });
    }

    @Test
    void testValidateDeviceType_Inactive() {
        // Given
        when(masterDataService.getDeviceTypeByCode("OLD_TYPE"))
                .thenReturn(Optional.of(inactiveDeviceType));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateDeviceType("OLD_TYPE");
        });
    }

    @Test
    void testValidateDeviceType_NullCode() {
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateDeviceType(null);
        });
    }

    @Test
    void testValidateManufacturer_Success() {
        // Given
        when(masterDataService.getManufacturerByCode("MAHINDRA"))
                .thenReturn(Optional.of(activeManufacturer));

        // When & Then - Should not throw
        assertDoesNotThrow(() -> {
            validationService.validateManufacturer("MAHINDRA");
        });
    }

    @Test
    void testValidateManufacturer_NotFound() {
        // Given
        when(masterDataService.getManufacturerByCode("INVALID"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateManufacturer("INVALID");
        });
    }

    @Test
    void testValidateManufacturer_Inactive() {
        // Given
        when(masterDataService.getManufacturerByCode("OLD_MFG"))
                .thenReturn(Optional.of(inactiveManufacturer));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateManufacturer("OLD_MFG");
        });
    }

    @Test
    void testValidateDeviceTypeAndManufacturer_Success() {
        // Given
        when(masterDataService.getDeviceTypeByCode("TRACTOR"))
                .thenReturn(Optional.of(activeDeviceType));
        when(masterDataService.getManufacturerByCode("MAHINDRA"))
                .thenReturn(Optional.of(activeManufacturer));

        // When & Then - Should not throw
        assertDoesNotThrow(() -> {
            validationService.validateDeviceTypeAndManufacturer("TRACTOR", "MAHINDRA");
        });
    }

    @Test
    void testValidateDeviceTypeAndManufacturer_DeviceTypeInvalid() {
        // Given
        when(masterDataService.getDeviceTypeByCode("INVALID"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            validationService.validateDeviceTypeAndManufacturer("INVALID", "MAHINDRA");
        });
        verify(masterDataService, never()).getManufacturerByCode(any());
    }
}

