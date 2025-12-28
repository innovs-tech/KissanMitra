package com.kissanmitra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * User profile information.
 *
 * <p>Contains user's personal information and default location.
 * Location is stored as GeoJSON Point for geospatial queries.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserProfile {

    /**
     * User's full name.
     */
    private String name;

    /**
     * Default location as GeoJSON Point.
     * Used for discovery and location-based features.
     * Coordinates: [longitude, latitude]
     */
    private GeoJsonPoint defaultLocation;

    /**
     * Pincode for the default location.
     */
    private String pincode;
}

