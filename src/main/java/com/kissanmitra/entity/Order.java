package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Handler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Order entity representing intent and fulfillment requests.
 *
 * <p>Business Context:
 * - Orders represent user intent (lease or rent)
 * - Orders are separate from Leases (which represent execution)
 * - Order type is automatically derived from thresholds
 *
 * <p>Uber Logic:
 * - Created from discovery intent after login
 * - Routed to Admin (LEASE) or VLE (RENT)
 * - State machine enforces valid transitions
 * - Leads to Lease creation for LEASE orders
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "orders")
public class Order extends BaseEntity {

    /**
     * Order type (LEASE or RENT).
     * Automatically derived from requested hours/acres and thresholds.
     */
    private OrderType orderType;

    /**
     * Current order status.
     * Follows state machine transitions.
     */
    private OrderStatus status;

    /**
     * Reference to device for which order is placed.
     */
    @Indexed
    private String deviceId;

    /**
     * User ID who requested the order.
     */
    @Indexed
    private String requestedBy;

    /**
     * Handler responsible for processing the order.
     * ADMIN for LEASE, VLE for RENT.
     */
    private Handler handledBy;

    /**
     * Requested hours of operation.
     */
    private Double requestedHours;

    /**
     * Requested acres to be covered.
     */
    private Double requestedAcres;

    /**
     * Optional note for the order.
     */
    private String note;

    /**
     * Requester's phone number.
     * Retrieved from current user context.
     */
    private String phone;

    /**
     * Requester's name.
     * Retrieved from user profile if available.
     */
    private String name;

    /**
     * Order start date.
     * Provided by frontend.
     */
    private java.time.LocalDate startDate;

    /**
     * Order end date.
     * Provided by frontend.
     */
    private java.time.LocalDate endDate;
}

