package com.kissanmitra.aspect;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect for automatic audit logging of all POST, PUT, PATCH, and DELETE operations.
 *
 * <p>Business Context:
 * - Automatically logs all create, update, and delete operations
 * - Extracts entity information from method parameters and return values
 * - Provides audit trail for compliance and dispute resolution
 *
 * <p>Uber Logic:
 * - Intercepts all @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping methods
 * - Extracts entity type and ID from method parameters or return values
 * - Logs audit events with appropriate action (CREATED, UPDATED, DELETED)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Intercepts POST methods to audit CREATE operations.
     */
    @AfterReturning(
            pointcut = "@annotation(org.springframework.web.bind.annotation.PostMapping)",
            returning = "result"
    )
    public void auditPostOperation(final JoinPoint joinPoint, final Object result) {
        try {
            // Skip if method should not be audited
            if (shouldSkipAudit(joinPoint)) {
                log.debug("Skipping audit for POST: Method excluded - {}", joinPoint.getSignature());
                return;
            }

            // Extract entity information from return value
            final EntityInfo entityInfo = extractEntityInfo(result, joinPoint.getArgs());
            if (entityInfo == null) {
                log.debug("Skipping audit for POST: Could not extract entity info from {} - result type: {}", 
                        joinPoint.getSignature(), result != null ? result.getClass().getSimpleName() : "null");
                return;
            }

            // Log CREATE action
            auditService.logEvent(
                    entityInfo.entityType,
                    entityInfo.entityId,
                    "CREATED",
                    null, // fromState
                    getEntityState(result), // toState
                    buildNote(joinPoint, "Created")
            );

            log.info("Audit logged: CREATED {} {} via {}", entityInfo.entityType, entityInfo.entityId, joinPoint.getSignature().toShortString());
        } catch (Exception e) {
            log.error("Error auditing POST operation: {}", e.getMessage(), e);
            // Don't throw exception - audit failure shouldn't break the operation
        }
    }

    /**
     * Intercepts PUT methods to audit UPDATE operations.
     */
    @AfterReturning(
            pointcut = "@annotation(org.springframework.web.bind.annotation.PutMapping)",
            returning = "result"
    )
    public void auditPutOperation(final JoinPoint joinPoint, final Object result) {
        try {
            if (shouldSkipAudit(joinPoint)) {
                return;
            }

            final EntityInfo entityInfo = extractEntityInfo(result, joinPoint.getArgs());
            if (entityInfo == null) {
                log.debug("Skipping audit for PUT: Could not extract entity info from {}", joinPoint.getSignature());
                return;
            }

            // Extract previous state from method arguments (if available)
            final String fromState = extractPreviousState(joinPoint.getArgs());

            auditService.logEvent(
                    entityInfo.entityType,
                    entityInfo.entityId,
                    "UPDATED",
                    fromState,
                    getEntityState(result),
                    buildNote(joinPoint, "Updated")
            );

            log.debug("Audit logged: UPDATED {} {}", entityInfo.entityType, entityInfo.entityId);
        } catch (Exception e) {
            log.error("Error auditing PUT operation: {}", e.getMessage(), e);
        }
    }

    /**
     * Intercepts PATCH methods to audit PARTIAL_UPDATE operations.
     */
    @AfterReturning(
            pointcut = "@annotation(org.springframework.web.bind.annotation.PatchMapping)",
            returning = "result"
    )
    public void auditPatchOperation(final JoinPoint joinPoint, final Object result) {
        try {
            if (shouldSkipAudit(joinPoint)) {
                return;
            }

            final EntityInfo entityInfo = extractEntityInfo(result, joinPoint.getArgs());
            if (entityInfo == null) {
                log.debug("Skipping audit for PATCH: Could not extract entity info from {}", joinPoint.getSignature());
                return;
            }

            final String fromState = extractPreviousState(joinPoint.getArgs());

            auditService.logEvent(
                    entityInfo.entityType,
                    entityInfo.entityId,
                    "PARTIAL_UPDATE",
                    fromState,
                    getEntityState(result),
                    buildNote(joinPoint, "Partially updated")
            );

            log.debug("Audit logged: PARTIAL_UPDATE {} {}", entityInfo.entityType, entityInfo.entityId);
        } catch (Exception e) {
            log.error("Error auditing PATCH operation: {}", e.getMessage(), e);
        }
    }

    /**
     * Intercepts DELETE methods to audit DELETE operations.
     */
    @Before("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void auditDeleteOperation(final JoinPoint joinPoint) {
        try {
            if (shouldSkipAudit(joinPoint)) {
                return;
            }

            // For DELETE, extract entity info from method arguments (before deletion)
            final EntityInfo entityInfo = extractEntityInfo(null, joinPoint.getArgs());
            if (entityInfo == null) {
                log.debug("Skipping audit for DELETE: Could not extract entity info from {}", joinPoint.getSignature());
                return;
            }

            final String fromState = extractPreviousState(joinPoint.getArgs());

            auditService.logEvent(
                    entityInfo.entityType,
                    entityInfo.entityId,
                    "DELETED",
                    fromState,
                    "DELETED",
                    buildNote(joinPoint, "Deleted")
            );

            log.debug("Audit logged: DELETED {} {}", entityInfo.entityType, entityInfo.entityId);
        } catch (Exception e) {
            log.error("Error auditing DELETE operation: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts entity information from method return value or parameters.
     *
     * @param result method return value (null for DELETE)
     * @param args method arguments
     * @return entity info or null if cannot extract
     */
    private EntityInfo extractEntityInfo(final Object result, final Object[] args) {
        // Try to extract from return value first (for POST, PUT, PATCH)
        if (result != null) {
            // Check if result is wrapped in BaseClientResponse
            if (result instanceof BaseClientResponse) {
                final BaseClientResponse<?> response = (BaseClientResponse<?>) result;
                final Object data = response.getData();
                if (data != null && data instanceof BaseEntity) {
                    return extractFromEntity((BaseEntity) data);
                }
            }

            // Direct entity return
            if (result instanceof BaseEntity) {
                return extractFromEntity((BaseEntity) result);
            }
        }

        // Try to extract from method arguments (for DELETE or when result is not entity)
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof BaseEntity) {
                    return extractFromEntity((BaseEntity) arg);
                }
                // Check for @PathVariable String id - try to infer entity type from controller
                if (arg instanceof String && args.length > 0) {
                    // Try to find entity type from method signature or controller class
                    final String entityType = inferEntityTypeFromContext(args, result);
                    if (entityType != null) {
                        return new EntityInfo(entityType, (String) arg);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extracts entity info from a BaseEntity.
     */
    private EntityInfo extractFromEntity(final BaseEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }

        final String entityType = entity.getClass().getSimpleName()
                .replace("Entity", "")
                .toUpperCase();
        return new EntityInfo(entityType, entity.getId());
    }

    /**
     * Infers entity type from method context (controller class name, method name, etc.).
     */
    private String inferEntityTypeFromContext(final Object[] args, final Object result) {
        // This is a fallback - ideally entity should be passed directly
        // Can be enhanced by analyzing method signature or controller class name
        // For now, return null and let the caller handle it
        return null;
    }

    /**
     * Extracts previous state from method arguments (for UPDATE operations).
     */
    private String extractPreviousState(final Object[] args) {
        // Try to find an entity in arguments that might have a status field
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof BaseEntity) {
                    try {
                        final Method getStatus = arg.getClass().getMethod("getStatus");
                        final Object status = getStatus.invoke(arg);
                        if (status != null) {
                            return status.toString();
                        }
                    } catch (Exception e) {
                        // Status method doesn't exist or failed - ignore
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets entity state from result object.
     */
    private String getEntityState(final Object result) {
        if (result == null) {
            return null;
        }

        // Unwrap BaseClientResponse if needed
        Object entity = result;
        if (result instanceof BaseClientResponse) {
            final BaseClientResponse<?> response = (BaseClientResponse<?>) result;
            entity = response.getData();
        }

        if (entity instanceof BaseEntity) {
            try {
                final Method getStatus = entity.getClass().getMethod("getStatus");
                final Object status = getStatus.invoke(entity);
                if (status != null) {
                    return status.toString();
                }
            } catch (Exception e) {
                // Status method doesn't exist - return null
            }
        }

        return null;
    }

    /**
     * Builds a note for the audit log.
     */
    private String buildNote(final JoinPoint joinPoint, final String action) {
        return String.format("%s via %s", action, joinPoint.getSignature().toShortString());
    }

    /**
     * Determines if audit should be skipped for this method.
     * Skips authentication endpoints and other non-entity operations.
     */
    private boolean shouldSkipAudit(final JoinPoint joinPoint) {
        final String className = joinPoint.getTarget().getClass().getSimpleName();
        final String methodName = joinPoint.getSignature().getName();

        // Skip auth operations
        if (className.contains("AuthController")) {
            return true;
        }

        // Skip discovery/public endpoints (GET operations)
        if (className.contains("DiscoveryController") || className.contains("MasterDataController")) {
            return true;
        }

        // Skip GET operations (already handled by method-level annotations)
        if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("search")) {
            return true;
        }

        // Note: File upload endpoints are NOT skipped - they modify entities and should be audited
        // (e.g., uploadMedia modifies Device entity)

        return false;
    }

    /**
     * Helper class to hold entity information.
     */
    private static class EntityInfo {
        final String entityType;
        final String entityId;

        EntityInfo(final String entityType, final String entityId) {
            this.entityType = entityType;
            this.entityId = entityId;
        }
    }
}

