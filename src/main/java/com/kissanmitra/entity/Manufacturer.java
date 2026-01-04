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
 * Master data entity for equipment manufacturers.
 *
 * <p>Represents manufacturers/vendors of equipment (e.g., Mahindra, John Deere).
 * Used as reference data for devices.
 *
 * <p>Business Context:
 * - Code is immutable identifier (e.g., "MAHINDRA", "JOHN_DEERE")
 * - Name is editable display value (e.g., "Mahindra Tractors")
 * - Code is used as reference in Device.manufacturerId
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "manufacturers")
public class Manufacturer extends BaseEntity {

    /**
     * Manufacturer code (e.g., MAHINDRA, JOHN_DEERE).
     * Immutable unique identifier used as reference in Device.manufacturerId.
     */
    @Indexed(unique = true)
    private String code;

    /**
     * Manufacturer display name (e.g., "Mahindra Tractors", "John Deere India").
     * Editable value that can be updated without breaking references.
     */
    private String name;

    /**
     * Whether this manufacturer is active and available for use.
     */
    private Boolean active;
}

