package com.kissanmitra.repository;

import com.kissanmitra.entity.PricingRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}

