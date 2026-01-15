package com.kissanmitra.response;

import com.kissanmitra.dto.Address;
import com.kissanmitra.dto.OperationalState;
import com.kissanmitra.entity.PricingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for device detail view.
 *
 * <p>Business Context:
 * - Public endpoint for viewing comprehensive device details
 * - Used when user selects a device from discovery page
 * - Includes all device information needed for detail view
 *
 * <p>Note: Coordinates (lat/lng) are NOT exposed to end users.
 * Only address and pincode are shown.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceDetailResponse {

    /**
     * Device ID.
     */
    private String deviceId;

    /**
     * Basic device details.
     */
    private BasicDetails basicDetails;

    /**
     * Media information.
     */
    private MediaInfo media;

    /**
     * Location information (address and pincode only, no coordinates).
     */
    private LocationInfo location;

    /**
     * Pricing information.
     */
    private PricingInfo pricing;

    /**
     * Lease information.
     */
    private LeaseInfo lease;

    /**
     * Distance information (optional - only if user location provided).
     */
    private DistanceInfo distance;

    /**
     * Operational information (if available).
     */
    private OperationalInfo operational;

    /**
     * Basic device details with enriched master data.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BasicDetails {
        private String name;
        private String description;
        private DeviceTypeInfo deviceType;
        private ManufacturerInfo manufacturer;
        private String owner;
        private String manufacturedDate;
        private Boolean companyOwned;
        private Boolean requiresOperator;
    }

    /**
     * Device type information with enriched display name.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DeviceTypeInfo {
        private String code;
        private String displayName;
    }

    /**
     * Manufacturer information with enriched name.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ManufacturerInfo {
        private String code;
        private String name;
    }

    /**
     * Media information.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MediaInfo {
        private Integer count;
        private String primaryUrl;
        private List<String> allUrls;
    }

    /**
     * Location information (address and pincode only).
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LocationInfo {
        private Address address;
        private String pincode;
    }

    /**
     * Pricing information.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PricingInfo {
        private PricingRule defaultRule;
        private List<PricingRule> timeSpecificRules;
    }

    /**
     * Lease information.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LeaseInfo {
        private String leaseState; // "LEASED" or "AVAILABLE"
        private String currentLeaseId; // null if available
    }

    /**
     * Distance information (optional).
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DistanceInfo {
        private Double distanceKm; // null if user location not provided
    }

    /**
     * Operational information (if available).
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OperationalInfo {
        private OperationalState operationalState; // null if not available
    }
}

