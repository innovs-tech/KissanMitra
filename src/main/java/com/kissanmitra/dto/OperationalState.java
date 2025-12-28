package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.DeviceHealth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Operational state of a device.
 *
 * <p>Contains real-time device information from telemetry.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperationalState {

    /**
     * Timestamp of last telemetry received from device.
     */
    private Instant lastSeenAt;

    /**
     * Battery level percentage (0-100).
     */
    private Integer battery;

    /**
     * Device health status.
     */
    private DeviceHealth health;
}

