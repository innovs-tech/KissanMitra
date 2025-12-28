package com.kissanmitra.domain.enums;

/**
 * Represents the source of telemetry data.
 *
 * <p>Used to track origin of metrics for audit and reconciliation.
 */
public enum TelemetrySource {
    /**
     * Telemetry data from device sensors.
     * Primary source of operational metrics.
     */
    DEVICE,

    /**
     * Manually entered telemetry data.
     * Used for corrections or offline scenarios.
     */
    MANUAL
}

