package com.kissanmitra.domain.enums;

/**
 * Represents the health status of a device.
 *
 * <p>Used in Device.operationalState.health to indicate device condition.
 * Derived from telemetry data and operational monitoring.
 */
public enum DeviceHealth {
    /**
     * Device is operating normally.
     */
    OK,

    /**
     * Device has minor issues or warnings.
     * May require attention but still operational.
     */
    WARNING,

    /**
     * Device has critical errors.
     * May require immediate attention or maintenance.
     */
    ERROR
}

