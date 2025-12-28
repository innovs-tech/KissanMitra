package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.ThresholdConfig;

import java.util.List;

/**
 * Service interface for pricing and threshold management.
 */
public interface PricingService {

    /**
     * Derives order type based on requested hours/acres and thresholds.
     *
     * @param deviceTypeId device type ID
     * @param requestedHours requested hours
     * @param requestedAcres requested acres
     * @return OrderType (LEASE or RENT)
     */
    OrderType deriveOrderType(String deviceTypeId, Double requestedHours, Double requestedAcres);

    /**
     * Gets active pricing rules for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @return list of pricing rules
     */
    List<PricingRule> getPricingRules(String deviceTypeId, String pincode);

    /**
     * Gets active threshold config for device type.
     *
     * @param deviceTypeId device type ID
     * @return threshold config
     */
    ThresholdConfig getThresholdConfig(String deviceTypeId);
}

