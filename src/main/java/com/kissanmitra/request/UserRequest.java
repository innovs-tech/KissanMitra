package com.kissanmitra.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Request DTO for user profile updates.
 *
 * <p>All fields are optional - supports partial updates.
 * Android app can send either defaultLocation (if permission granted)
 * or pincode (if permission denied).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserRequest {
    /**
     * Phone number to identify the user.
     * Required for user lookup.
     */
    private String phoneNumber;

    /**
     * User's full name.
     * Optional - only updates if provided.
     */
    private String name;

    /**
     * Default location as GeoJSON Point.
     * Used when location permission is granted.
     * Format: { "type": "Point", "coordinates": [longitude, latitude] }
     * Optional - only updates if provided.
     */
    private GeoJsonPoint defaultLocation;

    /**
     * Pincode for the default location.
     * Used when location permission is denied.
     * Optional - only updates if provided.
     */
    private String pincode;
}
