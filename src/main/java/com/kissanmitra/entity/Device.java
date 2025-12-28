package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.DeviceStatus;
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

/**
 * Device entity representing physical agricultural equipment.
 *
 * <p>Business Context:
 * - Devices are owned by Company (Phase 1)
 * - Devices can be leased to VLEs
 * - Devices emit telemetry for usage tracking
 *
 * <p>Uber Logic:
 * - Devices are registered by Admin
 * - Only WORKING devices appear in discovery
 * - Device location drives geospatial search
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
     * Only WORKING devices appear in discovery.
     */
    private DeviceStatus status;

    /**
     * Device location as Point (longitude, latitude).
     * Used for geospatial queries in discovery.
     * Supports GeoJSON format: { "type": "Point", "coordinates": [longitude, latitude] }
     */
    @GeoSpatialIndexed
    @JsonDeserialize(using = PointDeserializer.class)
    private Point location;

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

