package com.kissanmitra.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for discovery search.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscoveryResponse {

    /**
     * List of discovered devices.
     */
    private List<DeviceResult> results;

    /**
     * Device result in discovery.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DeviceResult {
        private String deviceId;
        private String deviceType;
        private Double distanceKm;
        private LocationInfo location;
        private String leaseState;
        private IndicativeRate indicativeRate;
        private String intentType;
        private List<String> allowedActions;
    }

    /**
     * Location information.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LocationInfo {
        private String pincode;
    }

    /**
     * Indicative pricing rate.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IndicativeRate {
        private String type;
        private Double amount;
    }
}

