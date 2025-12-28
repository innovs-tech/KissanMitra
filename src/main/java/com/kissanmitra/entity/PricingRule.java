package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.dto.PricingRuleItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * Pricing rule entity for equipment pricing.
 *
 * <p>Business Context:
 * - Pricing rules are configured per device type and pincode
 * - Multiple pricing metrics can be defined (per hour, per acre)
 * - Rules have effective date ranges
 *
 * <p>Uber Logic:
 * - Used to calculate indicative pricing in discovery
 * - Used for final billing calculations
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "pricing_rules")
public class PricingRule extends BaseEntity {

    /**
     * Reference to DeviceType.
     */
    @Indexed
    private String deviceTypeId;

    /**
     * Pincode for location-based pricing.
     */
    @Indexed
    private String pincode;

    /**
     * Pricing rules (per hour, per acre, etc.).
     */
    private List<PricingRuleItem> rules;

    /**
     * Effective from date.
     */
    private LocalDate effectiveFrom;

    /**
     * Effective to date (null if ongoing).
     */
    private LocalDate effectiveTo;

    /**
     * Rule status (ACTIVE, INACTIVE).
     */
    private String status;
}

