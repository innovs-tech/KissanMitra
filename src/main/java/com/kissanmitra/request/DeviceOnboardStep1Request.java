package com.kissanmitra.request;

import com.kissanmitra.dto.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.geo.Point;

/**
 * Request DTO for Step 1 of device onboarding: Basic Equipment Details Entry.
 *
 * <p>Business Context:
 * - Admin enters basic device information
 * - Address is geocoded to lat/lng by Android Google SDK
 * - Pincode is extracted from address for pricing lookup
 *
 * <p>Uber Logic:
 * - Validates required fields (name, owner, deviceTypeId, manufacturerId, address, location)
 * - Creates device with DRAFT status
 * - Returns deviceId for subsequent steps
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceOnboardStep1Request {

    /**
     * Unique sensor identifier from physical device.
     */
    private String sensorId;

    /**
     * Device display name (required, max 100 chars).
     */
    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String name;

    /**
     * Device description (optional, max 500 chars).
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Reference to DeviceType master data (required).
     */
    @NotBlank(message = "Device type is required")
    private String deviceTypeId;

    /**
     * Reference to Manufacturer master data (required).
     */
    @NotBlank(message = "Manufacturer is required")
    private String manufacturerId;

    /**
     * Owner name (required).
     */
    @NotBlank(message = "Owner name is required")
    private String owner;

    /**
     * Manufactured date in MM/YYYY format (optional).
     * Format: "01/2020"
     */
    private String manufacturedDate;

    /**
     * Structured address from Android Google SDK (required).
     */
    @NotNull(message = "Address is required")
    @Valid
    private Address address;

    /**
     * Device location as Point (longitude, latitude) (required).
     * Provided by Android SDK geocoding.
     * Supports GeoJSON format: { "type": "Point", "coordinates": [longitude, latitude] }
     */
    @NotNull(message = "Location is required")
    private Point location;

    /**
     * Whether device is owned by company.
     */
    private Boolean companyOwned;

    /**
     * Whether this device requires an operator.
     */
    private Boolean requiresOperator;
}

