package com.kissanmitra.service;

import com.kissanmitra.entity.Operator;
import com.kissanmitra.repository.OperatorRepository;
import com.kissanmitra.service.impl.OperatorServiceImpl;
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
 * Unit tests for OperatorService.
 *
 * <p>Tests operator creation, retrieval, and updates.
 */
@ExtendWith(MockitoExtension.class)
class OperatorServiceImplTest {

    @Mock
    private OperatorRepository operatorRepository;

    @InjectMocks
    private OperatorServiceImpl operatorService;

    private static final String TEST_OPERATOR_ID = "operator-id";
    private static final String TEST_USER_ID = "user-id";
    private Operator testOperator;

    @BeforeEach
    void setUp() {
        testOperator = Operator.builder()
                .id(TEST_OPERATOR_ID)
                .userId(TEST_USER_ID)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testCreateOperator_Success() {
        // Given
        when(operatorRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final Operator created = operatorService.createOperator(testOperator);

        // Then
        assertNotNull(created);
        verify(operatorRepository, times(1)).findByUserId(TEST_USER_ID);
        verify(operatorRepository, times(1)).save(testOperator);
    }

    @Test
    void testCreateOperator_DuplicateUserId() {
        // Given
        when(operatorRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(testOperator));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            operatorService.createOperator(testOperator);
        });

        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    void testGetOperatorById_Success() {
        // Given
        when(operatorRepository.findById(TEST_OPERATOR_ID)).thenReturn(Optional.of(testOperator));

        // When
        final Operator found = operatorService.getOperatorById(TEST_OPERATOR_ID);

        // Then
        assertNotNull(found);
        assertEquals(TEST_OPERATOR_ID, found.getId());
        verify(operatorRepository, times(1)).findById(TEST_OPERATOR_ID);
    }

    @Test
    void testGetOperatorById_NotFound() {
        // Given
        when(operatorRepository.findById(TEST_OPERATOR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            operatorService.getOperatorById(TEST_OPERATOR_ID);
        });
    }

    @Test
    void testGetOperatorByUserId_Success() {
        // Given
        when(operatorRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(testOperator));

        // When
        final Operator found = operatorService.getOperatorByUserId(TEST_USER_ID);

        // Then
        assertNotNull(found);
        assertEquals(TEST_USER_ID, found.getUserId());
    }

    @Test
    void testGetOperatorByUserId_NotFound() {
        // Given
        when(operatorRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When
        final Operator found = operatorService.getOperatorByUserId(TEST_USER_ID);

        // Then
        assertNull(found);
    }

    @Test
    void testGetActiveOperators() {
        // Given
        final List<Operator> activeOperators = Arrays.asList(testOperator);
        when(operatorRepository.findByStatus("ACTIVE")).thenReturn(activeOperators);

        // When
        final List<Operator> result = operatorService.getActiveOperators();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(operatorRepository, times(1)).findByStatus("ACTIVE");
    }

    @Test
    void testUpdateOperator_Success() {
        // Given
        final Operator updatedOperator = testOperator.toBuilder()
                .status("INACTIVE")
                .build();

        when(operatorRepository.findById(TEST_OPERATOR_ID)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final Operator result = operatorService.updateOperator(updatedOperator);

        // Then
        assertNotNull(result);
        assertEquals("INACTIVE", result.getStatus());
        verify(operatorRepository, times(1)).findById(TEST_OPERATOR_ID);
        verify(operatorRepository, times(1)).save(any(Operator.class));
    }

    @Test
    void testUpdateOperator_NoId() {
        // Given
        final Operator operatorWithoutId = Operator.builder()
                .userId(TEST_USER_ID)
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            operatorService.updateOperator(operatorWithoutId);
        });

        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    void testUpdateOperator_NotFound() {
        // Given
        when(operatorRepository.findById(TEST_OPERATOR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            operatorService.updateOperator(testOperator);
        });
    }

    @Test
    void testUpdateOperator_PartialUpdate() {
        // Given
        final Operator updatedOperator = Operator.builder()
                .id(TEST_OPERATOR_ID)
                .status("INACTIVE")
                // training is null - should keep existing
                .build();

        when(operatorRepository.findById(TEST_OPERATOR_ID)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final Operator result = operatorService.updateOperator(updatedOperator);

        // Then
        assertNotNull(result);
        assertEquals("INACTIVE", result.getStatus());
        // training should be preserved from existing operator
    }
}

