package com.kissanmitra.service;

/**
 * Service for validating master data references.
 *
 * <p>Business Context:
 * - Validates deviceTypeId and manufacturerId exist and are active
 * - Used in device onboarding and updates
 * - Prevents invalid references
 */
public interface MasterDataValidationService {

    /**
     * Validates device type code exists and is active.
     *
     * @param deviceTypeCode device type code
     * @throws RuntimeException if code doesn't exist or is inactive
     */
    void validateDeviceType(String deviceTypeCode);

    /**
     * Validates manufacturer code exists and is active.
     *
     * @param manufacturerCode manufacturer code
     * @throws RuntimeException if code doesn't exist or is inactive
     */
    void validateManufacturer(String manufacturerCode);

    /**
     * Validates both device type and manufacturer.
     *
     * @param deviceTypeCode device type code
     * @param manufacturerCode manufacturer code
     * @throws RuntimeException if either code doesn't exist or is inactive
     */
    void validateDeviceTypeAndManufacturer(String deviceTypeCode, String manufacturerCode);
}

