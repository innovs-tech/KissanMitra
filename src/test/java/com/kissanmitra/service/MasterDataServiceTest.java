package com.kissanmitra.service;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.repository.DeviceTypeRepository;
import com.kissanmitra.repository.ManufacturerRepository;
import com.kissanmitra.service.impl.MasterDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MasterDataService.
 *
 * <p>Tests caching, batch fetching, and lookup methods.
 */
@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @InjectMocks
    private MasterDataServiceImpl masterDataService;

    private DeviceType testDeviceType;
    private Manufacturer testManufacturer;

    @BeforeEach
    void setUp() {
        testDeviceType = DeviceType.builder()
                .id("type-id")
                .code("TRACTOR")
                .displayName("Heavy Duty Tractor")
                .active(true)
                .build();

        testManufacturer = Manufacturer.builder()
                .code("MAHINDRA")
                .name("Mahindra Tractors")
                .active(true)
                .build();
        testManufacturer.setId("mfg-id");
    }

    @Test
    void testGetDeviceTypeByCode_Success() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));

        // When
        var result = masterDataService.getDeviceTypeByCode("TRACTOR");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TRACTOR", result.get().getCode());
        verify(deviceTypeRepository).findByCode("TRACTOR");
    }

    @Test
    void testGetDeviceTypeByCode_NotFound() {
        // Given
        when(deviceTypeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When
        var result = masterDataService.getDeviceTypeByCode("INVALID");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDeviceTypeByCode_NullCode() {
        // When
        var result = masterDataService.getDeviceTypeByCode(null);

        // Then
        assertTrue(result.isEmpty());
        verify(deviceTypeRepository, never()).findByCode(any());
    }

    @Test
    void testGetDeviceTypeByCode_CaseInsensitive() {
        // Given
        when(deviceTypeRepository.findByCode("TRACTOR")).thenReturn(Optional.of(testDeviceType));

        // When
        var result = masterDataService.getDeviceTypeByCode("tractor");

        // Then
        assertTrue(result.isPresent());
        verify(deviceTypeRepository).findByCode("TRACTOR"); // Normalized to uppercase
    }

    @Test
    void testGetManufacturerByCode_Success() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));

        // When
        var result = masterDataService.getManufacturerByCode("MAHINDRA");

        // Then
        assertTrue(result.isPresent());
        assertEquals("MAHINDRA", result.get().getCode());
    }

    @Test
    void testGetDeviceTypesByCodes_BatchFetch() {
        // Given
        DeviceType type1 = DeviceType.builder().code("TRACTOR").displayName("Tractor").build();
        DeviceType type2 = DeviceType.builder().code("HARVESTER").displayName("Harvester").build();
        when(deviceTypeRepository.findByCodeIn(List.of("TRACTOR", "HARVESTER")))
                .thenReturn(List.of(type1, type2));

        // When
        Map<String, DeviceType> result = masterDataService.getDeviceTypesByCodes(
                List.of("TRACTOR", "HARVESTER")
        );

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("TRACTOR"));
        assertTrue(result.containsKey("HARVESTER"));
    }

    @Test
    void testGetDeviceTypesByCodes_EmptyList() {
        // When
        Map<String, DeviceType> result = masterDataService.getDeviceTypesByCodes(List.of());

        // Then
        assertTrue(result.isEmpty());
        verify(deviceTypeRepository, never()).findByCodeIn(any());
    }

    @Test
    void testGetDeviceTypesByCodes_NullList() {
        // When
        Map<String, DeviceType> result = masterDataService.getDeviceTypesByCodes(null);

        // Then
        assertTrue(result.isEmpty());
        verify(deviceTypeRepository, never()).findByCodeIn(any());
    }

    @Test
    void testGetManufacturersByCodes_BatchFetch() {
        // Given
        Manufacturer mfg1 = Manufacturer.builder().code("MAHINDRA").name("Mahindra").build();
        Manufacturer mfg2 = Manufacturer.builder().code("JOHN_DEERE").name("John Deere").build();
        when(manufacturerRepository.findByCodeIn(List.of("MAHINDRA", "JOHN_DEERE")))
                .thenReturn(List.of(mfg1, mfg2));

        // When
        Map<String, Manufacturer> result = masterDataService.getManufacturersByCodes(
                List.of("MAHINDRA", "JOHN_DEERE")
        );

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("MAHINDRA"));
        assertTrue(result.containsKey("JOHN_DEERE"));
    }
}

