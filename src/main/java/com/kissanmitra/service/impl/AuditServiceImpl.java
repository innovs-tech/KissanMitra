package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.entity.AuditLog;
import com.kissanmitra.repository.AuditLogRepository;
import com.kissanmitra.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service implementation for audit logging.
 *
 * <p>Business Context:
 * - All state transitions are audited
 * - Audit logs are immutable
 * - Used for compliance and dispute resolution
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserContext userContext;

    /**
     * Logs an audit event.
     *
     * @param entityType entity type
     * @param entityId entity ID
     * @param action action performed
     * @param fromState previous state (nullable)
     * @param toState new state
     * @param note optional note
     */
    @Override
    public void logEvent(
            final String entityType,
            final String entityId,
            final String action,
            final String fromState,
            final String toState,
            final String note
    ) {
        final String actorId = userContext.getCurrentUserId();
        if (actorId == null) {
            log.warn("Cannot log audit event: user not authenticated");
            return;
        }

        final AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .fromState(fromState)
                .toState(toState)
                .actorId(actorId)
                .timestamp(Instant.now())
                .note(note)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {} -> {}", entityType, entityId, fromState, toState);
    }
}

