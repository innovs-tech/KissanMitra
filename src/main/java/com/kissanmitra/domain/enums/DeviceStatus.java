package com.kissanmitra.domain.enums;

/**
 * Represents the operational status of a device.
 *
 * <p>Only WORKING devices appear in discovery.
 * Status is managed by Admin and affects equipment availability.
 */
public enum DeviceStatus {
    /**
     * Device is operational and eligible for discovery and leasing.
     */
    WORKING,

    /**
     * Device is temporarily unavailable.
     * Not shown in discovery, cannot be leased.
     */
    NOT_WORKING,

    /**
     * Device is permanently removed from service.
     * Cannot be leased or discovered.
     */
    RETIRED
}

