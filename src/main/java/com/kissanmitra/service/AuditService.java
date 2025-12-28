package com.kissanmitra.service;

import com.kissanmitra.entity.AuditLog;

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
}

