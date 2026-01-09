package com.kissanmitra.service;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.entity.AuditLog;
import com.kissanmitra.repository.AuditLogRepository;
import com.kissanmitra.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 *
 * <p>Tests audit log creation and validation.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private AuditServiceImpl auditService;

    private static final String TEST_USER_ID = "user-id";
    private static final String TEST_ENTITY_TYPE = "ORDER";
    private static final String TEST_ENTITY_ID = "entity-id";

    @BeforeEach
    void setUp() {
        when(userContext.getCurrentUserId()).thenReturn(TEST_USER_ID);
    }

    @Test
    void testLogEvent_Success() {
        // Given
        final String action = "CREATED";
        final String fromState = null;
        final String toState = "ACTIVE";
        final String note = "Test note";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(TEST_ENTITY_TYPE, TEST_ENTITY_ID, action, fromState, toState, note);

        // Then
        final ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        final AuditLog saved = captor.getValue();
        assertEquals(TEST_ENTITY_TYPE, saved.getEntityType());
        assertEquals(TEST_ENTITY_ID, saved.getEntityId());
        assertEquals(action, saved.getAction());
        assertEquals(fromState, saved.getFromState());
        assertEquals(toState, saved.getToState());
        assertEquals(TEST_USER_ID, saved.getActorId());
        assertEquals(note, saved.getNote());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testLogEvent_WithStateTransition() {
        // Given
        final String fromState = "PENDING";
        final String toState = "APPROVED";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(TEST_ENTITY_TYPE, TEST_ENTITY_ID, "STATUS_CHANGED", fromState, toState, null);

        // Then
        final ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        final AuditLog saved = captor.getValue();
        assertEquals(fromState, saved.getFromState());
        assertEquals(toState, saved.getToState());
    }

    @Test
    void testLogEvent_UserNotAuthenticated() {
        // Given
        when(userContext.getCurrentUserId()).thenReturn(null);

        // When
        auditService.logEvent(TEST_ENTITY_TYPE, TEST_ENTITY_ID, "CREATED", null, null, null);

        // Then
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void testLogCreate() {
        // Given
        final String note = "Created via test";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logCreate(TEST_ENTITY_TYPE, TEST_ENTITY_ID, note);

        // Then
        final ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        final AuditLog saved = captor.getValue();
        assertEquals("CREATED", saved.getAction());
        assertEquals(note, saved.getNote());
    }

    @Test
    void testLogUpdate() {
        // Given
        final String fromState = "PENDING";
        final String toState = "APPROVED";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logUpdate(TEST_ENTITY_TYPE, TEST_ENTITY_ID, fromState, toState, null);

        // Then
        final ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        final AuditLog saved = captor.getValue();
        assertEquals("UPDATED", saved.getAction());
        assertEquals(fromState, saved.getFromState());
        assertEquals(toState, saved.getToState());
    }

    @Test
    void testLogDelete() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logDelete(TEST_ENTITY_TYPE, TEST_ENTITY_ID, "Deleted via test");

        // Then
        final ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        final AuditLog saved = captor.getValue();
        assertEquals("DELETED", saved.getAction());
        assertEquals("DELETED", saved.getToState());
    }
}

