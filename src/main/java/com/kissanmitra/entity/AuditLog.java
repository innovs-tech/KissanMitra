package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Audit log entity for tracking all state transitions and critical actions.
 *
 * <p>Business Context:
 * - Immutable audit records for compliance
 * - Tracks all order and lease state transitions
 * - Enables audit trail and dispute resolution
 *
 * <p>Uber Logic:
 * - Created automatically on state transitions
 * - Never modified or deleted
 * - Indexed for efficient querying
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "audit_logs")
public class AuditLog extends BaseEntity {

    /**
     * Entity type (ORDER, LEASE, etc.).
     */
    @Indexed
    private String entityType;

    /**
     * Entity ID.
     */
    @Indexed
    private String entityId;

    /**
     * Action performed (STATUS_CHANGED, CREATED, etc.).
     */
    private String action;

    /**
     * Previous state (nullable).
     */
    private String fromState;

    /**
     * New state.
     */
    private String toState;

    /**
     * Actor who performed the action (user ID).
     */
    @Indexed
    private String actorId;

    /**
     * Timestamp of the action.
     */
    private Instant timestamp;

    /**
     * Optional note for the action.
     */
    private String note;
}

