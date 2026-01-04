package com.kissanmitra.service.impl;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.repository.DeviceTypeRepository;
import com.kissanmitra.repository.ManufacturerRepository;
import com.kissanmitra.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of MasterDataService with caching.
 *
 * <p>Business Context:
 * - Caches DeviceType and Manufacturer lookups
 * - Cache eviction on updates (handled by controllers)
 * - Batch fetching for performance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataServiceImpl implements MasterDataService {

    private static final String DEVICE_TYPE_CACHE = "deviceTypes";
    private static final String MANUFACTURER_CACHE = "manufacturers";

    private final DeviceTypeRepository deviceTypeRepository;
    private final ManufacturerRepository manufacturerRepository;

    @Override
    @Cacheable(value = DEVICE_TYPE_CACHE, key = "#code.toUpperCase()")
    public Optional<DeviceType> getDeviceTypeByCode(final String code) {
        if (code == null) {
            return Optional.empty();
        }
        return deviceTypeRepository.findByCode(code.toUpperCase());
    }

    @Override
    @Cacheable(value = MANUFACTURER_CACHE, key = "#code.toUpperCase()")
    public Optional<Manufacturer> getManufacturerByCode(final String code) {
        if (code == null) {
            return Optional.empty();
        }
        return manufacturerRepository.findByCode(code.toUpperCase());
    }

    @Override
    public Map<String, DeviceType> getDeviceTypesByCodes(final List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }

        // Normalize codes to uppercase
        final List<String> normalizedCodes = codes.stream()
                .filter(code -> code != null && !code.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }

        // Batch fetch from repository
        final List<DeviceType> deviceTypes = deviceTypeRepository.findByCodeIn(normalizedCodes);

        // Convert to map (cache will be populated by individual lookups)
        return deviceTypes.stream()
                .collect(Collectors.toMap(
                        DeviceType::getCode,
                        dt -> dt,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));
    }

    @Override
    public Map<String, Manufacturer> getManufacturersByCodes(final List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }

        // Normalize codes to uppercase
        final List<String> normalizedCodes = codes.stream()
                .filter(code -> code != null && !code.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }

        // Batch fetch from repository
        final List<Manufacturer> manufacturers = manufacturerRepository.findByCodeIn(normalizedCodes);

        // Convert to map (cache will be populated by individual lookups)
        return manufacturers.stream()
                .collect(Collectors.toMap(
                        Manufacturer::getCode,
                        m -> m,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));
    }

    /**
     * Evicts device type from cache.
     * Called by controllers after updates/deletes.
     *
     * @param code device type code
     */
    @CacheEvict(value = DEVICE_TYPE_CACHE, key = "#code.toUpperCase()")
    public void evictDeviceTypeCache(final String code) {
        log.debug("Evicted device type from cache: {}", code);
    }

    /**
     * Evicts manufacturer from cache.
     * Called by controllers after updates/deletes.
     *
     * @param code manufacturer code
     */
    @CacheEvict(value = MANUFACTURER_CACHE, key = "#code.toUpperCase()")
    public void evictManufacturerCache(final String code) {
        log.debug("Evicted manufacturer from cache: {}", code);
    }
}

