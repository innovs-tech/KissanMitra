package com.kissanmitra.domain.enums;

/**
 * Represents the pricing metric used in pricing rules.
 *
 * <p>Equipment can be priced per hour of operation or per acre covered.
 * Used in PricingRule.rules to define rate structure.
 */
public enum PricingMetric {
    /**
     * Pricing per hour of operation.
     */
    PER_HOUR,

    /**
     * Pricing per acre covered.
     */
    PER_ACRE
}

