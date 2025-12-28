package com.kissanmitra.repository;

import com.kissanmitra.entity.DeviceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DeviceType entity.
 */
@Repository
public interface DeviceTypeRepository extends MongoRepository<DeviceType, String> {

    /**
     * Finds a device type by code.
     *
     * @param code device type code
     * @return Optional DeviceType
     */
    Optional<DeviceType> findByCode(String code);
}

