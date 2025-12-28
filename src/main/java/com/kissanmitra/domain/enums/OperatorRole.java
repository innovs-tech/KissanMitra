package com.kissanmitra.domain.enums;

/**
 * Represents the role of an operator assigned to a lease.
 *
 * <p>Operators are assigned to leases (not orders or devices).
 * Metrics attribution defaults to PRIMARY operator.
 * If PRIMARY becomes unavailable, SECONDARY is promoted.
 */
public enum OperatorRole {
    /**
     * Primary operator responsible for equipment operation.
     * Receives default metric attribution.
     */
    PRIMARY,

    /**
     * Secondary/backup operator.
     * Promoted to PRIMARY if primary becomes unavailable.
     */
    SECONDARY
}

