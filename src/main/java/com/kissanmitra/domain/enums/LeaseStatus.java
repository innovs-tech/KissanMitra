package com.kissanmitra.domain.enums;

/**
 * Represents the lifecycle state of a lease.
 *
 * <p>Leases are created from approved LEASE orders and represent
 * long-term equipment control from Company to VLE.
 */
public enum LeaseStatus {
    /**
     * Lease is active and equipment is in use.
     * Telemetry is being collected and attributed to operators.
     */
    ACTIVE,

    /**
     * Lease period has ended normally.
     * Equipment returned, billing finalized.
     */
    COMPLETED,

    /**
     * Lease terminated early.
     * May occur due to breach or mutual agreement.
     */
    TERMINATED
}

