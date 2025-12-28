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
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "manufacturers")
public class Manufacturer extends BaseEntity {

    /**
     * Manufacturer name (e.g., Mahindra, John Deere).
     * Unique identifier for the manufacturer.
     */
    @Indexed(unique = true)
    private String name;

    /**
     * Whether this manufacturer is active and available for use.
     */
    private Boolean active;
}

