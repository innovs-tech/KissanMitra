package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.HandlerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the handler responsible for processing an order.
 *
 * <p>Used in Order.handledBy to route orders:
 * <ul>
 *   <li>ADMIN - Handles LEASE orders</li>
 *   <li>VLE - Handles RENT orders</li>
 * </ul>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Handler {

    /**
     * Type of handler (ADMIN or VLE).
     */
    private HandlerType type;

    /**
     * ID of the handler (adminId or vleId).
     */
    private String id;
}

