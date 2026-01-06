package com.kissanmitra.service;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.entity.Manufacturer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for master data lookups with caching.
 *
 * <p>Business Context:
 * - Provides cached lookups for DeviceType and Manufacturer
 * - Batch fetching for performance optimization
 * - Used in response enrichment
 */
public interface MasterDataService {

    /**
     * Gets device type by code (cached).
     *
     * @param code device type code
     * @return Optional DeviceType
     */
    Optional<DeviceType> getDeviceTypeByCode(String code);

    /**
     * Gets manufacturer by code (cached).
     *
     * @param code manufacturer code
     * @return Optional Manufacturer
     */
    Optional<Manufacturer> getManufacturerByCode(String code);

    /**
     * Batch fetch device types by codes (cached).
     *
     * @param codes list of device type codes
     * @return map of code to DeviceType
     */
    Map<String, DeviceType> getDeviceTypesByCodes(List<String> codes);

    /**
     * Batch fetch manufacturers by codes (cached).
     *
     * @param codes list of manufacturer codes
     * @return map of code to Manufacturer
     */
    Map<String, Manufacturer> getManufacturersByCodes(List<String> codes);

    /**
     * Evicts device type from cache.
     * Called by controllers after updates/deletes.
     *
     * @param code device type code
     */
    void evictDeviceTypeCache(String code);

    /**
     * Evicts manufacturer from cache.
     * Called by controllers after updates/deletes.
     *
     * @param code manufacturer code
     */
    void evictManufacturerCache(String code);
}

