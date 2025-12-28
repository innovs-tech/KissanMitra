package com.kissanmitra.domain.enums;

/**
 * Represents the type of commitment for a lease.
 *
 * <p>Leases can be committed by hours of operation or acres covered.
 * Used in Lease.commitment to specify the commitment metric.
 */
public enum CommitmentType {
    /**
     * Commitment measured in hours of operation.
     */
    HOURS,

    /**
     * Commitment measured in acres covered.
     */
    ACRES
}

