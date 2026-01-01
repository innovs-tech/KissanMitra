package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.dto.Address;
import com.kissanmitra.dto.OperationalState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kissanmitra.config.PointDeserializer;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * Device entity representing physical agricultural equipment.
 *
 * <p>Business Context:
 * - Devices are owned by Company (Phase 1)
 * - Devices can be leased to VLEs
 * - Devices emit telemetry for usage tracking
 * - Devices go through 4-step onboarding flow
 *
 * <p>Uber Logic:
 * - Devices are registered by Admin through onboarding flow
 * - Only LIVE devices with active pricing rules appear in discovery
 * - Device location drives geospatial search
 * - Address and pincode used for pricing lookup
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "devices")
public class Device extends BaseEntity {

    /**
     * Unique sensor identifier from physical device.
     * Used for telemetry ingestion and device identification.
     */
    @Indexed(unique = true)
    private String sensorId;

    /**
     * Device display name (required, max 100 chars).
     * Shown in discovery and listings.
     */
    private String name;

    /**
     * Device description (optional, max 500 chars).
     * Detailed information about the device.
     */
    private String description;

    /**
     * Owner name (required).
     * Name of the person/company owning the device.
     */
    private String owner;

    /**
     * Manufactured date in MM/YYYY format (optional).
     * Format: "01/2020"
     */
    private String manufacturedDate;

    /**
     * Reference to DeviceType master data.
     */
    @Field("deviceTypeId")
    private String deviceTypeId;

    /**
     * Reference to Manufacturer master data.
     */
    @Field("manufacturerId")
    private String manufacturerId;

    /**
     * Whether device is owned by company.
     * Phase 1: All devices are company-owned.
     */
    private Boolean companyOwned;

    /**
     * Whether this device requires an operator.
     * Determined by device type but can be overridden.
     */
    private Boolean requiresOperator;

    /**
     * Device operational status.
     * Only LIVE devices with active pricing rules appear in discovery.
     */
    private DeviceStatus status;

    /**
     * Structured address from Android Google SDK.
     * Contains firstLine, secondLine, pinCode, city, state, country.
     */
    private Address address;

    /**
     * Pincode extracted from address.pinCode.
     * Used for pricing lookup along with deviceTypeId.
     */
    @Indexed
    private String pincode;

    /**
     * Device location as Point (longitude, latitude).
     * Used for geospatial queries in discovery.
     * Supports GeoJSON format: { "type": "Point", "coordinates": [longitude, latitude] }
     * Provided by Android SDK geocoding.
     */
    @GeoSpatialIndexed
    @JsonDeserialize(using = PointDeserializer.class)
    private Point location;

    /**
     * URLs of uploaded media files (images/videos from S3).
     * Max 20 files total (photos + videos combined).
     * Formats: JPG, PNG, MP4, MOV.
     */
    private List<String> mediaUrls;

    /**
     * Primary/thumbnail media URL.
     * One media must be marked as primary (shown in listing).
     */
    private String primaryMediaUrl;

    /**
     * Current lease ID if device is leased.
     * Null if device is available.
     */
    @Indexed
    private String currentLeaseId;

    /**
     * Operational state from telemetry.
     * Updated asynchronously from device metrics.
     */
    private OperationalState operationalState;
}

