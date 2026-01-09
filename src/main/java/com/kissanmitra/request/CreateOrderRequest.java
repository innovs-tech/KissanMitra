package com.kissanmitra.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating an order.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRequest {

    /**
     * Device ID for which order is placed.
     */
    @NotNull(message = "Device ID is required")
    private String deviceId;

    /**
     * Requested hours of operation.
     */
    private Double requestedHours;

    /**
     * Requested acres to be covered.
     */
    private Double requestedAcres;

    /**
     * Optional note.
     */
    private String note;

    /**
     * Intent ID if order is created from discovery intent.
     */
    private String intentId;

    /**
     * Order start date.
     * Required for order creation.
     */
    @NotNull(message = "Start date is required")
    private java.time.LocalDate startDate;

    /**
     * Order end date.
     * Required for order creation.
     */
    @NotNull(message = "End date is required")
    private java.time.LocalDate endDate;
}

