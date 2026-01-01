package com.kissanmitra.response;

import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.dto.Address;
import com.kissanmitra.entity.PricingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for device onboarding summary.
 *
 * <p>Business Context:
 * - Step 4 of onboarding flow
 * - Shows all entered information in read-only format
 * - Used for review before finalizing
 *
 * <p>Note: Coordinates (lat/lng) are NOT exposed to end user.
 * Only address and pincode are shown.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceOnboardSummaryResponse {

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
     * Device status.
     */
    private DeviceStatus status;

    /**
     * Basic device details.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BasicDetails {
        private String name;
        private String description;
        private String deviceTypeId;
        private String manufacturerId;
        private String owner;
        private String manufacturedDate;
        private Boolean companyOwned;
        private Boolean requiresOperator;
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
        private List<String> urls;
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
}

