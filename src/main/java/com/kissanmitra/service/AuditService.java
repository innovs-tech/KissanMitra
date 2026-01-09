package com.kissanmitra.service;

/**
 * Service interface for audit logging.
 */
public interface AuditService {

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
    void logEvent(String entityType, String entityId, String action, String fromState, String toState, String note);

    /**
     * Logs a CREATE operation.
     *
     * @param entityType entity type
     * @param entityId entity ID
     * @param note optional note
     */
    default void logCreate(String entityType, String entityId, String note) {
        logEvent(entityType, entityId, "CREATED", null, null, note);
    }

    /**
     * Logs an UPDATE operation.
     *
     * @param entityType entity type
     * @param entityId entity ID
     * @param fromState previous state (nullable)
     * @param toState new state (nullable)
     * @param note optional note
     */
    default void logUpdate(String entityType, String entityId, String fromState, String toState, String note) {
        logEvent(entityType, entityId, "UPDATED", fromState, toState, note);
    }

    /**
     * Logs a DELETE operation.
     *
     * @param entityType entity type
     * @param entityId entity ID
     * @param note optional note
     */
    default void logDelete(String entityType, String entityId, String note) {
        logEvent(entityType, entityId, "DELETED", null, "DELETED", note);
    }
}

