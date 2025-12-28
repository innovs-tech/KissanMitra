package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.PricingMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pricing rule item within a PricingRule.
 *
 * <p>Stored as nested object in PricingRule.rules array.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingRuleItem {

    /**
     * Pricing metric (PER_HOUR or PER_ACRE).
     */
    private PricingMetric metric;

    /**
     * Rate for the metric.
     */
    private Double rate;
}

