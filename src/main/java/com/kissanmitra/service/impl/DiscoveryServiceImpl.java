package com.kissanmitra.service.impl;

import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.service.DiscoveryService;
import com.kissanmitra.service.PricingService;
import com.kissanmitra.request.DiscoverySearchRequest;
import com.kissanmitra.response.DiscoveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for discovery operations.
 *
 * <p>Business Context:
 * - Discovery is public and unauthenticated
 * - Location-based search using geospatial queries
 * - Intent capture for pre-login interest
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final int INTENT_TTL_MINUTES = 30;
    private static final String INTENT_STATUS_CREATED = "CREATED";
    private static final double DEFAULT_RADIUS_KM = 50.0; // Default search radius
    private static final int DEFAULT_PAGE_SIZE = 10; // Default pagination
    private static final int MAX_PAGE_SIZE = 20; // Maximum pagination

    private final DeviceRepository deviceRepository;
    private final DiscoveryIntentRepository discoveryIntentRepository;
    private final PricingService pricingService;

    /**
     * Searches for devices based on location and filters.
     *
     * <p>Business Context:
     * - Uses lat/lng for both customer location and device location
     * - Default search radius: 50km (configurable)
     * - Only LIVE devices with active pricing rules appear in discovery
     * - Pagination: default 10 per page, max 20 per page
     *
     * <p>Uber Logic:
     * - Filters by status = LIVE
     * - Filters by active pricing rule exists (double validation)
     * - Calculates distance using Haversine formula
     * - Uses device pincode for pricing lookup
     * - Excludes devices without pricing rules from results
     *
     * @param request discovery search request
     * @return discovery response with device results
     */
    @Override
    public DiscoveryResponse searchDevices(final DiscoverySearchRequest request) {
        // Convert location to Point
        final Point location = getLocationPoint(request.getLocation());
        if (location == null) {
            throw new RuntimeException("Location is required (lat/lng)");
        }

        // Calculate search radius (default 50km)
        final double radiusKm = request.getFilters() != null && request.getFilters().getRadiusKm() != null
                ? request.getFilters().getRadiusKm()
                : DEFAULT_RADIUS_KM;

        final Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);

        // BUSINESS DECISION: Only LIVE devices appear in discovery
        // Search devices near location with status = LIVE
        List<Device> devices = deviceRepository.findByLocationNearAndStatus(
                location,
                distance,
                DeviceStatus.LIVE
        );

        // Filter by device type if specified
        if (request.getFilters() != null && request.getFilters().getDeviceType() != null) {
            devices = devices.stream()
                    .filter(d -> d.getDeviceTypeId() != null
                            && d.getDeviceTypeId().equals(request.getFilters().getDeviceType()))
                    .collect(Collectors.toList());
        }

        // BUSINESS DECISION: Double validation - filter by active pricing rule exists
        // Devices without pricing rules are excluded from results (even if status is LIVE)
        devices = devices.stream()
                .filter(device -> {
                    if (device.getDeviceTypeId() == null || device.getPincode() == null) {
                        return false;
                    }
                    // Check if active pricing rule exists
                    final boolean hasPricingRule = pricingService.hasActivePricingRule(
                            device.getDeviceTypeId(), device.getPincode());
                    if (!hasPricingRule) {
                        log.debug("Excluding device {} from discovery - no active pricing rule", device.getId());
                    }
                    return hasPricingRule;
                })
                .collect(Collectors.toList());

        // Apply pagination
        final int pageSize = getPageSize(request);
        final int page = getPage(request);
        final int start = page * pageSize;
        final int end = Math.min(start + pageSize, devices.size());
        final List<Device> pagedDevices = devices.subList(Math.min(start, devices.size()), end);

        // Convert to response
        final List<DiscoveryResponse.DeviceResult> results = pagedDevices.stream()
                .map(device -> mapToDeviceResult(device, location, request))
                .collect(Collectors.toList());

        return DiscoveryResponse.builder()
                .results(results)
                .totalCount(devices.size())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    /**
     * Gets page size from request (default 10, max 20).
     */
    private int getPageSize(final DiscoverySearchRequest request) {
        if (request.getFilters() == null || request.getFilters().getPageSize() == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(1, request.getFilters().getPageSize()), MAX_PAGE_SIZE);
    }

    /**
     * Gets page number from request (default 0).
     */
    private int getPage(final DiscoverySearchRequest request) {
        if (request.getFilters() == null || request.getFilters().getPage() == null) {
            return 0;
        }
        return Math.max(0, request.getFilters().getPage());
    }

    /**
     * Creates a discovery intent for pre-login interest.
     *
     * @param deviceId device ID
     * @param intentType intent type (RENT/LEASE)
     * @param requestedHours requested hours
     * @param requestedAcres requested acres
     * @return intent ID
     */
    @Override
    public String createIntent(
            final String deviceId,
            final String intentType,
            final Double requestedHours,
            final Double requestedAcres
    ) {
        final DiscoveryIntent intent = DiscoveryIntent.builder()
                .deviceId(deviceId)
                .intentType(intentType)
                .requestedHours(requestedHours)
                .requestedAcres(requestedAcres)
                .status(INTENT_STATUS_CREATED)
                .expiresAt(Instant.now().plusSeconds(INTENT_TTL_MINUTES * 60L))
                .build();

        final DiscoveryIntent saved = discoveryIntentRepository.save(intent);
        log.info("Created discovery intent: {} for device: {}", saved.getId(), deviceId);
        return saved.getId();
    }

    private Point getLocationPoint(final DiscoverySearchRequest.LocationInput location) {
        if (location == null) {
            return null;
        }

        if (location.getLat() != null && location.getLng() != null) {
            // GPS coordinates: MongoDB uses (lng, lat) format
            return new Point(location.getLng(), location.getLat());
        }

        // TODO: Convert pincode to coordinates using geocoding service
        // For now, pincode-based search is not fully implemented
        return null;
    }

    private DiscoveryResponse.DeviceResult mapToDeviceResult(
            final Device device,
            final Point searchLocation,
            final DiscoverySearchRequest request
    ) {
        // Calculate distance (simplified)
        final double distanceKm = calculateDistance(
                searchLocation.getX(), searchLocation.getY(),
                device.getLocation().getX(), device.getLocation().getY()
        );

        // Get indicative pricing
        final DiscoveryResponse.IndicativeRate indicativeRate = getIndicativeRate(device);

        // Determine intent type if requested
        String intentType = null;
        if (request.getIntent() != null) {
            try {
                final com.kissanmitra.domain.enums.OrderType orderType = pricingService.deriveOrderType(
                        device.getDeviceTypeId(),
                        request.getIntent().getRequestedHours(),
                        request.getIntent().getRequestedAcres()
                );
                intentType = orderType.name();
            } catch (Exception e) {
                log.warn("Could not derive order type: {}", e.getMessage());
            }
        }

        return DiscoveryResponse.DeviceResult.builder()
                .deviceId(device.getId())
                .deviceType(device.getDeviceTypeId())
                .distanceKm(distanceKm)
                .location(DiscoveryResponse.LocationInfo.builder()
                        .pincode(device.getPincode()) // BUSINESS DECISION: Use device.pincode (not "N/A")
                        .build())
                .leaseState(device.getCurrentLeaseId() != null ? "LEASED" : "AVAILABLE")
                .indicativeRate(indicativeRate)
                .intentType(intentType)
                .allowedActions(List.of("SHOW_INTEREST"))
                .build();
    }

    private double calculateDistance(final double lng1, final double lat1, final double lng2, final double lat2) {
        // Haversine formula for distance calculation
        final double earthRadius = 6371; // km
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLng = Math.toRadians(lng2 - lng1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Gets indicative pricing rate for device.
     *
     * <p>Business Decision:
     * - Uses device.pincode for pricing lookup (not "N/A")
     * - Returns null if no active rule found (not 0.0)
     * - Devices without pricing rules are excluded from discovery results
     *
     * @param device device to get pricing for
     * @return indicative rate or null
     */
    private DiscoveryResponse.IndicativeRate getIndicativeRate(final Device device) {
        if (device.getDeviceTypeId() == null || device.getPincode() == null) {
            log.warn("Device {} missing deviceTypeId or pincode", device.getId());
            return null;
        }

        try {
            // BUSINESS DECISION: Use device.pincode for pricing lookup
            final PricingRule activeRule = pricingService.getActivePricingForDevice(device.getId(), java.time.LocalDate.now());
            if (activeRule != null && activeRule.getRules() != null && !activeRule.getRules().isEmpty()) {
                final var firstRule = activeRule.getRules().get(0);
                return DiscoveryResponse.IndicativeRate.builder()
                        .type(firstRule.getMetric().name())
                        .amount(firstRule.getRate())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Could not get pricing for device {}: {}", device.getId(), e.getMessage());
        }

        // BUSINESS DECISION: Return null if no pricing rule found (not 0.0)
        // Devices without pricing rules are excluded from discovery results
        return null;
    }
}

