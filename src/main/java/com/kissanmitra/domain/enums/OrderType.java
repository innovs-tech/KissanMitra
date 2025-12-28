package com.kissanmitra.domain.enums;

/**
 * Represents the type of order.
 *
 * <p>Order type is automatically derived at creation time based on configured thresholds:
 * <ul>
 *   <li>If requestedHours ≤ maxRentalHours AND requestedAcres ≤ maxRentalAcres → RENT</li>
 *   <li>Else → LEASE</li>
 * </ul>
 *
 * <p>LEASE orders are handled by Admin and lead to long-term equipment control.
 * RENT orders are handled by VLE and represent short-term usage.
 */
public enum OrderType {
    /**
     * Long-term equipment leasing from Company to VLE.
     * Requires Admin approval and leads to lease creation.
     */
    LEASE,

    /**
     * Short-term equipment rental from VLE to Farmer.
     * Handled by VLE, equipment remains discoverable.
     */
    RENT
}

