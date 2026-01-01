package com.kissanmitra.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for discovery search.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscoverySearchRequest {

    /**
     * Location coordinates (latitude, longitude).
     */
    private LocationInput location;

    /**
     * Intent details (optional).
     */
    private IntentInput intent;

    /**
     * Search filters.
     */
    private SearchFilters filters;

    /**
     * Location input (GPS or pincode).
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LocationInput {
        private Double lat;
        private Double lng;
        private String pincode;
    }

    /**
     * Intent input for order type derivation.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IntentInput {
        private Double requestedHours;
        private Double requestedAcres;
    }

    /**
     * Search filters.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SearchFilters {
        private String deviceType;
        private Double radiusKm;
        private Integer page; // Page number (0-based, default 0)
        private Integer pageSize; // Page size (default 10, max 20)
    }
}

