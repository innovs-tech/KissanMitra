package com.kissanmitra.service.impl;

import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.ThresholdConfig;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for pricing and threshold management.
 *
 * <p>Business Context:
 * - Order type is automatically derived from thresholds
 * - Pricing rules are used for discovery and billing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ThresholdConfigRepository thresholdConfigRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Derives order type based on requested hours/acres and thresholds.
     *
     * <p>Business Decision:
     * - If requestedHours ≤ maxRentalHours AND requestedAcres ≤ maxRentalAcres → RENT
     * - Else → LEASE
     *
     * @param deviceTypeId device type ID
     * @param requestedHours requested hours
     * @param requestedAcres requested acres
     * @return OrderType (LEASE or RENT)
     */
    @Override
    public OrderType deriveOrderType(
            final String deviceTypeId,
            final Double requestedHours,
            final Double requestedAcres
    ) {
        final ThresholdConfig config = thresholdConfigRepository
                .findByDeviceTypeIdAndStatus(deviceTypeId, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("Threshold config not found for device type: " + deviceTypeId));

        // BUSINESS DECISION: Check both hours and acres thresholds
        final boolean isRental = (requestedHours == null || requestedHours <= config.getMaxRentalHours())
                && (requestedAcres == null || requestedAcres <= config.getMaxRentalAcres());

        return isRental ? OrderType.RENT : OrderType.LEASE;
    }

    /**
     * Gets active pricing rules for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @return list of pricing rules
     */
    @Override
    public List<PricingRule> getPricingRules(final String deviceTypeId, final String pincode) {
        final LocalDate now = LocalDate.now();
        return pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                deviceTypeId,
                pincode,
                ACTIVE_STATUS,
                now,
                now
        );
    }

    /**
     * Gets active threshold config for device type.
     *
     * @param deviceTypeId device type ID
     * @return threshold config
     */
    @Override
    public ThresholdConfig getThresholdConfig(final String deviceTypeId) {
        return thresholdConfigRepository
                .findByDeviceTypeIdAndStatus(deviceTypeId, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("Threshold config not found for device type: " + deviceTypeId));
    }

    @Override
    public PricingRule getPricingForDevice(final String deviceId) {
        return getActivePricingForDevice(deviceId, LocalDate.now());
    }

    @Override
    public PricingRule getActivePricingForDevice(final String deviceId, final LocalDate date) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        if (device.getDeviceTypeId() == null || device.getPincode() == null) {
            log.warn("Device {} missing deviceTypeId or pincode", deviceId);
            return null;
        }

        // BUSINESS DECISION: Check time-specific rules first, then default rule
        // Time-specific rules take precedence if date matches
        final List<PricingRule> timeSpecificRules = getTimeSpecificRules(
                device.getDeviceTypeId(), device.getPincode(), date);

        if (!timeSpecificRules.isEmpty()) {
            // Return first matching time-specific rule
            return timeSpecificRules.get(0);
        }

        // Fall back to default rule
        return getDefaultRule(device.getDeviceTypeId(), device.getPincode());
    }

    @Override
    public boolean hasActivePricingRule(final String deviceTypeId, final String pincode) {
        final PricingRule defaultRule = getDefaultRule(deviceTypeId, pincode);
        return defaultRule != null;
    }

    @Override
    public List<PricingRule> checkForConflicts(final PricingRule newRule) {
        if (newRule.getEffectiveTo() == null) {
            // Default rule - check if one already exists
            final PricingRule existing = getDefaultRule(newRule.getDeviceTypeId(), newRule.getPincode());
            return existing != null ? List.of(existing) : new ArrayList<>();
        }

        // Time-specific rule - check for overlapping date ranges
        return pricingRuleRepository.findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNotNullAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                newRule.getDeviceTypeId(),
                newRule.getPincode(),
                ACTIVE_STATUS,
                newRule.getEffectiveTo(),
                newRule.getEffectiveFrom()
        );
    }

    @Override
    public PricingRule getDefaultRule(final String deviceTypeId, final String pincode) {
        return pricingRuleRepository
                .findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(deviceTypeId, pincode, ACTIVE_STATUS)
                .orElse(null);
    }

    @Override
    public List<PricingRule> getTimeSpecificRules(
            final String deviceTypeId, final String pincode, final LocalDate date) {
        return pricingRuleRepository
                .findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
                        deviceTypeId, pincode, ACTIVE_STATUS, date, date);
    }
}

