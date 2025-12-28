package com.kissanmitra.domain.enums;

/**
 * Represents the type of handler responsible for processing an order.
 *
 * <p>Used in Order.handledBy to route orders to the appropriate handler:
 * <ul>
 *   <li>ADMIN - Handles LEASE orders (Company → VLE)</li>
 *   <li>VLE - Handles RENT orders (VLE → Farmer)</li>
 * </ul>
 */
public enum HandlerType {
    /**
     * Admin user handles the order.
     * Used for LEASE orders.
     */
    ADMIN,

    /**
     * VLE handles the order.
     * Used for RENT orders.
     */
    VLE
}

