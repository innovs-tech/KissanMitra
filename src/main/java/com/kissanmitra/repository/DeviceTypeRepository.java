package com.kissanmitra.repository;

import com.kissanmitra.entity.DeviceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Finds device types by code list (batch fetch).
     *
     * @param codes list of device type codes
     * @return list of device types
     */
    List<DeviceType> findByCodeIn(List<String> codes);

    /**
     * Finds active device types.
     *
     * @param active active status
     * @return list of active device types
     */
    List<DeviceType> findByActive(Boolean active);
}

