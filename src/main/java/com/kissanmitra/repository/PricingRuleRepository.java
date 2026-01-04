package com.kissanmitra.repository;

import com.kissanmitra.entity.PricingRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PricingRule entity.
 */
@Repository
public interface PricingRuleRepository extends MongoRepository<PricingRule, String> {

    /**
     * Finds active pricing rules for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param status status
     * @param date current date
     * @return list of pricing rules
     */
    List<PricingRule> findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            String deviceTypeId,
            String pincode,
            String status,
            LocalDate date,
            LocalDate date2
    );

    /**
     * Finds default rule (effectiveTo = null) for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param status status
     * @return default rule or empty
     */
    Optional<PricingRule> findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNull(
            String deviceTypeId, String pincode, String status);

    /**
     * Finds active time-specific rules for date.
     * Rules where date is between effectiveFrom and effectiveTo (or effectiveTo is null).
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param date date to check
     * @param status status
     * @return list of active time-specific rules
     */
    List<PricingRule> findByDeviceTypeIdAndPincodeAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNotNullAndEffectiveToGreaterThanEqual(
            String deviceTypeId, String pincode, String status, LocalDate date, LocalDate date2);

    /**
     * Finds overlapping time-specific rules.
     * Rules where date ranges overlap with the given range.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param from start date
     * @param to end date
     * @param status status
     * @return list of overlapping rules
     */
    List<PricingRule> findByDeviceTypeIdAndPincodeAndStatusAndEffectiveToIsNotNullAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            String deviceTypeId, String pincode, String status, LocalDate to, LocalDate from);

    /**
     * Finds all active rules (default + time-specific) for device type and pincode.
     *
     * @param deviceTypeId device type ID
     * @param pincode pincode
     * @param status status
     * @return list of all active rules
     */
    List<PricingRule> findByDeviceTypeIdAndPincodeAndStatus(String deviceTypeId, String pincode, String status);

    /**
     * Counts pricing rules by device type code.
     *
     * @param deviceTypeId device type code
     * @return count of pricing rules
     */
    long countByDeviceTypeId(String deviceTypeId);
}

