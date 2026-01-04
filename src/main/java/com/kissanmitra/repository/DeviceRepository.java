package com.kissanmitra.repository;

import com.kissanmitra.entity.Device;
import com.kissanmitra.domain.enums.DeviceStatus;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Device entity.
 */
@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {

    /**
     * Finds device by sensor ID.
     *
     * @param sensorId sensor ID
     * @return Optional Device
     */
    Optional<Device> findBySensorId(String sensorId);

    /**
     * Finds devices near a location within a distance.
     *
     * @param location location point
     * @param distance maximum distance
     * @return list of devices
     */
    List<Device> findByLocationNearAndStatus(Point location, Distance distance, DeviceStatus status);

    /**
     * Finds devices by device type and status.
     *
     * @param deviceTypeId device type ID
     * @param status device status
     * @return list of devices
     */
    List<Device> findByDeviceTypeIdAndStatus(String deviceTypeId, DeviceStatus status);

    /**
     * Counts devices by device type code.
     *
     * @param deviceTypeId device type code
     * @return count of devices
     */
    long countByDeviceTypeId(String deviceTypeId);

    /**
     * Counts devices by manufacturer code.
     *
     * @param manufacturerId manufacturer code
     * @return count of devices
     */
    long countByManufacturerId(String manufacturerId);
}

