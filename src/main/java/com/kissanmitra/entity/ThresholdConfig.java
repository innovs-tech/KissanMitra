package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Threshold configuration for determining order type (LEASE vs RENT).
 *
 * <p>Business Context:
 * - Thresholds determine if a request is LEASE or RENT
 * - If requestedHours ≤ maxRentalHours AND requestedAcres ≤ maxRentalAcres → RENT
 * - Else → LEASE
 *
 * <p>Uber Logic:
 * - Configured per device type by Admin
 * - Used during order creation to derive order type
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "threshold_config")
public class ThresholdConfig extends BaseEntity {

    /**
     * Reference to DeviceType.
     */
    @Indexed(unique = true)
    private String deviceTypeId;

    /**
     * Maximum hours for rental (not lease).
     */
    private Integer maxRentalHours;

    /**
     * Maximum acres for rental (not lease).
     */
    private Integer maxRentalAcres;

    /**
     * Effective from date.
     */
    private LocalDate effectiveFrom;

    /**
     * Effective to date (null if ongoing).
     */
    private LocalDate effectiveTo;

    /**
     * Config status (ACTIVE, INACTIVE).
     */
    private String status;
}

