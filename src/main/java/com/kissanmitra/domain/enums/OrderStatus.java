package com.kissanmitra.domain.enums;

/**
 * Represents the lifecycle state of an order.
 *
 * <p>Order states follow a deterministic state machine:
 * <ul>
 *   <li>DRAFT → INTEREST_RAISED → UNDER_REVIEW → ACCEPTED → PICKUP_SCHEDULED → ACTIVE → COMPLETED → CLOSED</li>
 *   <li>Rental orders: DRAFT → INTEREST_RAISED → ACCEPTED → ACTIVE → COMPLETED → CLOSED</li>
 *   <li>Rejection: INTEREST_RAISED → REJECTED, UNDER_REVIEW → REJECTED (by handler)</li>
 *   <li>Cancellation: INTEREST_RAISED → CANCELLED (by requester)</li>
 * </ul>
 *
 * <p>State transitions are validated by {@link com.kissanmitra.service.OrderStateMachine}.
 */
public enum OrderStatus {
    /**
     * Temporary state before a request is submitted.
     * Not visible to Admin, can be discarded.
     */
    DRAFT,

    /**
     * Formal interest submitted by requester.
     * Equipment still visible in discovery, multiple interests allowed.
     */
    INTEREST_RAISED,

    /**
     * Request is actively being reviewed by Admin (for LEASE) or VLE (for RENT).
     */
    UNDER_REVIEW,

    /**
     * Request accepted but equipment not yet handed over.
     * Equipment becomes unavailable for discovery.
     */
    ACCEPTED,

    /**
     * Pickup date and logistics finalized.
     * Operator assignment required before activation.
     */
    PICKUP_SCHEDULED,

    /**
     * Equipment has been physically handed over.
     * Telemetry ingestion enabled, order becomes metric-bearing.
     */
    ACTIVE,

    /**
     * Usage period has ended.
     * Telemetry ingestion stops, aggregation finalized.
     */
    COMPLETED,

    /**
     * Final terminal state.
     * Order becomes read-only, billing finalized.
     */
    CLOSED,

    /**
     * Order rejected by handler (Admin for LEASE, VLE for RENT).
     * Terminal state - cannot be reactivated.
     */
    REJECTED,

    /**
     * Order cancelled by requester.
     * Terminal state - cannot be reactivated.
     */
    CANCELLED
}

