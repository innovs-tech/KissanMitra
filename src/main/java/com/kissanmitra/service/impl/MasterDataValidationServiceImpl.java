package com.kissanmitra.service.impl;

import com.kissanmitra.service.MasterDataService;
import com.kissanmitra.service.MasterDataValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of MasterDataValidationService.
 *
 * <p>Business Context:
 * - Uses MasterDataService for cached lookups
 * - Validates existence and active status
 * - Provides clear error messages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataValidationServiceImpl implements MasterDataValidationService {

    private final MasterDataService masterDataService;

    @Override
    public void validateDeviceType(final String deviceTypeCode) {
        if (deviceTypeCode == null || deviceTypeCode.trim().isEmpty()) {
            throw new RuntimeException("Device type code is required");
        }

        final var deviceType = masterDataService.getDeviceTypeByCode(deviceTypeCode);
        if (deviceType.isEmpty()) {
            throw new RuntimeException("Device type not found: " + deviceTypeCode);
        }

        if (Boolean.FALSE.equals(deviceType.get().getActive())) {
            throw new RuntimeException("Device type is inactive: " + deviceTypeCode);
        }
    }

    @Override
    public void validateManufacturer(final String manufacturerCode) {
        if (manufacturerCode == null || manufacturerCode.trim().isEmpty()) {
            throw new RuntimeException("Manufacturer code is required");
        }

        final var manufacturer = masterDataService.getManufacturerByCode(manufacturerCode);
        if (manufacturer.isEmpty()) {
            throw new RuntimeException("Manufacturer not found: " + manufacturerCode);
        }

        if (Boolean.FALSE.equals(manufacturer.get().getActive())) {
            throw new RuntimeException("Manufacturer is inactive: " + manufacturerCode);
        }
    }

    @Override
    public void validateDeviceTypeAndManufacturer(final String deviceTypeCode, final String manufacturerCode) {
        validateDeviceType(deviceTypeCode);
        validateManufacturer(manufacturerCode);
    }
}

