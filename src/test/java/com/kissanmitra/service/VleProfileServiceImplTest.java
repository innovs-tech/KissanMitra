package com.kissanmitra.service;

import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.service.impl.VleProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VleProfileService.
 *
 * <p>Tests VLE profile creation, retrieval, and management.
 */
@ExtendWith(MockitoExtension.class)
class VleProfileServiceImplTest {

    @Mock
    private VleProfileRepository vleProfileRepository;

    @InjectMocks
    private VleProfileServiceImpl vleProfileService;

    private static final String TEST_VLE_ID = "vle-id";
    private static final String TEST_USER_ID = "user-id";
    private VleProfile testVleProfile;

    @BeforeEach
    void setUp() {
        testVleProfile = VleProfile.builder()
                .id(TEST_VLE_ID)
                .userId(TEST_USER_ID)
                .name("Test Business")
                .build();
    }

    @Test
    void testCreateVleProfile_Success() {
        // Given
        when(vleProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
        when(vleProfileRepository.save(any(VleProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final VleProfile created = vleProfileService.createVleProfile(testVleProfile);

        // Then
        assertNotNull(created);
        verify(vleProfileRepository, times(1)).findByUserId(TEST_USER_ID);
        verify(vleProfileRepository, times(1)).save(testVleProfile);
    }

    @Test
    void testCreateVleProfile_DuplicateUserId() {
        // Given
        when(vleProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(testVleProfile));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            vleProfileService.createVleProfile(testVleProfile);
        });

        verify(vleProfileRepository, never()).save(any(VleProfile.class));
    }

    @Test
    void testGetVleProfileById_Success() {
        // Given
        when(vleProfileRepository.findById(TEST_VLE_ID)).thenReturn(Optional.of(testVleProfile));

        // When
        final VleProfile found = vleProfileService.getVleProfileById(TEST_VLE_ID);

        // Then
        assertNotNull(found);
        assertEquals(TEST_VLE_ID, found.getId());
        verify(vleProfileRepository, times(1)).findById(TEST_VLE_ID);
    }

    @Test
    void testGetVleProfileById_NotFound() {
        // Given
        when(vleProfileRepository.findById(TEST_VLE_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            vleProfileService.getVleProfileById(TEST_VLE_ID);
        });
    }

    @Test
    void testGetVleProfileByUserId_Success() {
        // Given
        when(vleProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(testVleProfile));

        // When
        final VleProfile found = vleProfileService.getVleProfileByUserId(TEST_USER_ID);

        // Then
        assertNotNull(found);
        assertEquals(TEST_USER_ID, found.getUserId());
    }

    @Test
    void testGetVleProfileByUserId_NotFound() {
        // Given
        when(vleProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When
        final VleProfile found = vleProfileService.getVleProfileByUserId(TEST_USER_ID);

        // Then
        assertNull(found);
    }

    @Test
    void testGetAllVleProfiles() {
        // Given
        final List<VleProfile> profiles = Arrays.asList(testVleProfile);
        when(vleProfileRepository.findAll()).thenReturn(profiles);

        // When
        final List<VleProfile> result = vleProfileService.getAllVleProfiles();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vleProfileRepository, times(1)).findAll();
    }
}

