package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.ThresholdConfig;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for pricing and threshold management.
 *
 * <p>Business Context:
 * - Pricing rules support default (ongoing) and time-specific (date range) rules
 * - Default rule: effectiveTo = null (always active)
 * - Time-specific rule: date range (e.g., peak season pricing)
 * - Resolution: Time-specific takes precedence if date matches, else default
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

    /**
     * Gets active pricing for device (default + time-specific).
     *
     * @param deviceId device ID
     * @return active pricing rule (time-specific if date matches, else default)
     */
    PricingRule getPricingForDevice(String deviceId);

    /**
     * Gets active pricing for device for specific date.
     *
     * <p>Business Decision:
     * - Checks for time-specific rule matching date first
     * - Falls back to default rule (effectiveTo = null) if no time-specific match
     * - Returns null if no rule found
     *
     * @param deviceId device ID
     * @param date date to check
     * @return active pricing rule or null
     */
    PricingRule getActivePricingForDevice(String deviceId, LocalDate date);

    /**
     * Checks if active pricing rule exists for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @return true if default rule exists
     */
    boolean hasActivePricingRule(String deviceTypeId, String pincode);

    /**
     * Checks for overlapping time-specific rules.
     *
     * <p>Business Decision:
     * - Detects overlapping date ranges for same deviceTypeId + pincode
     * - Used to warn admin before creating conflicting rules
     *
     * @param newRule new pricing rule to check
     * @return list of overlapping rules
     */
    List<PricingRule> checkForConflicts(PricingRule newRule);

    /**
     * Gets default rule (effectiveTo = null) for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @return default rule or null
     */
    PricingRule getDefaultRule(String deviceTypeId, String pincode);

    /**
     * Gets active time-specific rules for date.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param date date to check
     * @return list of active time-specific rules
     */
    List<PricingRule> getTimeSpecificRules(String deviceTypeId, String pincode, LocalDate date);
}

