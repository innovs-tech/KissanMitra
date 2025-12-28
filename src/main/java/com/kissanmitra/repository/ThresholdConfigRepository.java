package com.kissanmitra.repository;

import com.kissanmitra.entity.ThresholdConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ThresholdConfig entity.
 */
@Repository
public interface ThresholdConfigRepository extends MongoRepository<ThresholdConfig, String> {

    /**
     * Finds active threshold config for device type.
     *
     * @param deviceTypeId device type ID
     * @param status status
     * @return Optional ThresholdConfig
     */
    Optional<ThresholdConfig> findByDeviceTypeIdAndStatus(String deviceTypeId, String status);
}

