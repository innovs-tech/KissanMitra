package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Master data entity for device types.
 *
 * <p>Represents categories of equipment (TRACTOR, HARVESTER, etc.).
 * Used as reference data for devices and pricing rules.
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "device_types")
public class DeviceType extends BaseEntity {

    /**
     * Device type code (e.g., TRACTOR, HARVESTER).
     * Unique identifier for the device type.
     */
    @Indexed(unique = true)
    private String code;

    /**
     * Display name for the device type.
     */
    private String displayName;

    /**
     * Whether this device type requires an operator.
     */
    private Boolean requiresOperator;

    /**
     * Whether this device type is active and available for use.
     */
    private Boolean active;
}

