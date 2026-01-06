package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.ManufacturerRepository;
import com.kissanmitra.request.CreateManufacturerRequest;
import com.kissanmitra.request.UpdateManufacturerRequest;
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
 * Unit tests for ManufacturerController.
 *
 * <p>Tests CRUD operations, code immutability, and soft delete validation.
 */
@ExtendWith(MockitoExtension.class)
class ManufacturerControllerTest {

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MasterDataService masterDataService;

    @InjectMocks
    private ManufacturerController manufacturerController;

    private Manufacturer testManufacturer;
    private CreateManufacturerRequest createRequest;
    private UpdateManufacturerRequest updateRequest;

    @BeforeEach
    void setUp() {
        testManufacturer = Manufacturer.builder()
                .code("MAHINDRA")
                .name("Mahindra Tractors")
                .active(true)
                .build();
        testManufacturer.setId("mfg-id");

        createRequest = CreateManufacturerRequest.builder()
                .code("JOHN_DEERE")
                .name("John Deere India")
                .active(true)
                .build();

        updateRequest = UpdateManufacturerRequest.builder()
                .name("Mahindra & Mahindra")
                .active(true)
                .build();
    }

    @Test
    void testCreateManufacturer_Success() {
        // Given
        when(manufacturerRepository.findByCode("JOHN_DEERE")).thenReturn(Optional.empty());
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(testManufacturer);

        // When
        var result = manufacturerController.createManufacturer(createRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(manufacturerRepository).findByCode("JOHN_DEERE");
        verify(manufacturerRepository).save(any(Manufacturer.class));
    }

    @Test
    void testCreateManufacturer_DuplicateCode() {
        // Given
        when(manufacturerRepository.findByCode("JOHN_DEERE")).thenReturn(Optional.of(testManufacturer));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            manufacturerController.createManufacturer(createRequest);
        });
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
    }

    @Test
    void testGetAllManufacturers_WithActiveFilter() {
        // Given
        when(manufacturerRepository.findByActive(true)).thenReturn(List.of(testManufacturer));

        // When
        var result = manufacturerController.getAllManufacturers(true);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        verify(manufacturerRepository).findByActive(true);
    }

    @Test
    void testGetManufacturerByCode_Success() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));

        // When
        var result = manufacturerController.getManufacturerByCode("MAHINDRA");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("MAHINDRA", result.getData().getCode());
    }

    @Test
    void testUpdateManufacturer_Success() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(testManufacturer);

        // When
        var result = manufacturerController.updateManufacturer("MAHINDRA", updateRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(manufacturerRepository).save(any(Manufacturer.class));
    }

    @Test
    void testUpdateManufacturer_CodeImmutability() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(testManufacturer);

        // When
        var result = manufacturerController.updateManufacturer("MAHINDRA", updateRequest);

        // Then - Code should remain unchanged
        assertNotNull(result);
        verify(manufacturerRepository).save(argThat(manufacturer ->
                "MAHINDRA".equals(manufacturer.getCode()) // Code unchanged
        ));
    }

    @Test
    void testDeleteManufacturer_Success() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));
        when(deviceRepository.countByManufacturerId("MAHINDRA")).thenReturn(0L);
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(testManufacturer);

        // When
        var result = manufacturerController.deleteManufacturer("MAHINDRA");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(manufacturerRepository).save(argThat(manufacturer ->
                Boolean.FALSE.equals(manufacturer.getActive()) // Soft delete
        ));
    }

    @Test
    void testDeleteManufacturer_HasDeviceReferences() {
        // Given
        when(manufacturerRepository.findByCode("MAHINDRA")).thenReturn(Optional.of(testManufacturer));
        when(deviceRepository.countByManufacturerId("MAHINDRA")).thenReturn(10L);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            manufacturerController.deleteManufacturer("MAHINDRA");
        });
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
    }
}

