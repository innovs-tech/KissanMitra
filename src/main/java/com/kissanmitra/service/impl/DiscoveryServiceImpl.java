package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.DiscoveryIntent;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DiscoveryIntentRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.service.DiscoveryService;
import com.kissanmitra.service.MasterDataService;
import com.kissanmitra.service.MediaUploadService;
import com.kissanmitra.service.PricingService;
import com.kissanmitra.request.DiscoverySearchRequest;
import com.kissanmitra.response.DeviceDetailResponse;
import com.kissanmitra.response.DiscoveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for discovery operations.
 *
 * <p>Business Context:
 * - Discovery supports both authenticated and unauthenticated users
 * - Location-based search using geospatial queries
 * - Role-based device visibility (FARMER/VLE see leased, unauthenticated see unleased)
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
    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final MasterDataService masterDataService;
    private final MediaUploadService mediaUploadService;
    private final UserContext userContext;

    /**
     * Searches for devices based on location and filters.
     *
     * <p>Business Context:
     * - Uses lat/lng for both customer location and device location
     * - Default search radius: 50km (configurable)
     * - Only LIVE devices with active pricing rules appear in discovery
     * - Devices with orders in unavailable states (ACCEPTED, PICKUP_SCHEDULED, ACTIVE, COMPLETED) are excluded
     * - Role-based visibility: FARMER sees leased devices, VLE sees unleased devices, Unauthenticated/ADMIN see all devices
     * - Pagination: default 10 per page, max 20 per page
     *
     * <p>Uber Logic:
     * - Filters by status = LIVE
     * - Role-based filtering: FARMER → leased devices, VLE → unleased devices, Unauthenticated/ADMIN → all devices
     * - Filters by active pricing rule exists (double validation)
     * - Excludes devices with orders in unavailable states (ACCEPTED through COMPLETED)
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

        // BUSINESS DECISION: Role-based device visibility
        // - FARMER: Only see devices that are leased (currentLeaseId != null)
        // - VLE: Only see devices that are NOT leased (currentLeaseId == null) - to create LEASE orders
        // - Unauthenticated: See all devices (no lease filter)
        final String currentUserId = userContext.getCurrentUserId();
        if (currentUserId != null) {
            final UserRole activeRole = userContext.getCurrentUserActiveRole();
            if (activeRole == UserRole.FARMER) {
                // FARMER: Only see leased devices
                devices = devices.stream()
                        .filter(device -> device.getCurrentLeaseId() != null)
                        .collect(Collectors.toList());
                log.debug("Filtered devices for FARMER: showing only leased devices");
            } else if (activeRole == UserRole.VLE) {
                // VLE: Only see devices that are NOT leased (to create LEASE orders)
                devices = devices.stream()
                        .filter(device -> device.getCurrentLeaseId() == null)
                        .collect(Collectors.toList());
                log.debug("Filtered devices for VLE: showing only unleased devices");
            }
            // ADMIN and other roles: show all devices (no lease filter)
        }
        // Unauthenticated users: show all devices (no lease filter)

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

        // BUSINESS DECISION: Exclude devices with orders in unavailable states
        // Devices with ACCEPTED, PICKUP_SCHEDULED, ACTIVE, or COMPLETED orders are not discoverable
        // This prevents double-booking and ensures devices are only shown when truly available
        devices = devices.stream()
                .filter(device -> {
                    final List<Order> orders = orderRepository.findByDeviceId(device.getId());
                    if (orders == null || orders.isEmpty()) {
                        return true; // No orders = available
                    }
                    // Check if device has any order in unavailable states
                    final boolean hasUnavailableOrder = orders.stream()
                            .anyMatch(order -> order.getStatus() == OrderStatus.ACCEPTED
                                    || order.getStatus() == OrderStatus.PICKUP_SCHEDULED
                                    || order.getStatus() == OrderStatus.ACTIVE
                                    || order.getStatus() == OrderStatus.COMPLETED);
                    if (hasUnavailableOrder) {
                        log.debug("Excluding device {} from discovery - has unavailable order", device.getId());
                    }
                    return !hasUnavailableOrder;
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

        // FUTURE ENHANCEMENT: Convert pincode to coordinates using geocoding service
        // Current implementation supports lat/lng which is sufficient for mobile apps
        // Pincode geocoding would require integration with Google Maps Geocoding API or similar
        // For now, pincode-based search is not implemented - apps should provide lat/lng
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

    /**
     * Gets comprehensive device details for detail view.
     *
     * <p>Business Context:
     * - Public endpoint for viewing device details
     * - Only LIVE devices visible to public (ONBOARDED visible to authenticated users)
     * - Enriches deviceType and manufacturer with display names from master data
     * - Includes full pricing rules and media information
     *
     * <p>Uber Logic:
     * - Fetches device by ID
     * - Validates device status (LIVE for public, ONBOARDED for authenticated)
     * - Enriches with master data (DeviceType displayName, Manufacturer name)
     * - Gets pricing rules (default + time-specific for current date)
     * - Calculates distance if user location provided
     * - Builds comprehensive response with all device information
     *
     * @param deviceId device ID
     * @param userLat user's latitude (optional, for distance calculation)
     * @param userLng user's longitude (optional, for distance calculation)
     * @return device detail response
     */
    @Override
    public DeviceDetailResponse getDeviceDetails(final String deviceId, final Double userLat, final Double userLng) {
        log.info("Getting device details for deviceId: {}", deviceId);

        // Fetch device by ID
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found with id: " + deviceId));

        // BUSINESS DECISION: Only LIVE devices visible to public
        // ONBOARDED devices visible to authenticated users
        final String currentUserId = userContext.getCurrentUserId();
        if (device.getStatus() != DeviceStatus.LIVE) {
            if (device.getStatus() == DeviceStatus.ONBOARDED && currentUserId != null) {
                // Allow authenticated users to see ONBOARDED devices
                log.debug("Allowing authenticated user {} to view ONBOARDED device {}", currentUserId, deviceId);
            } else {
                throw new RuntimeException("Device is not available for viewing. Status: " + device.getStatus());
            }
        }

        // Enrich with master data
        DeviceDetailResponse.DeviceTypeInfo deviceTypeInfo = null;
        if (device.getDeviceTypeId() != null) {
            final var deviceTypeOpt = masterDataService.getDeviceTypeByCode(device.getDeviceTypeId());
            if (deviceTypeOpt.isPresent()) {
                final var deviceType = deviceTypeOpt.get();
                deviceTypeInfo = DeviceDetailResponse.DeviceTypeInfo.builder()
                        .code(deviceType.getCode())
                        .displayName(deviceType.getDisplayName())
                        .build();
            } else {
                // Fallback if master data not found
                deviceTypeInfo = DeviceDetailResponse.DeviceTypeInfo.builder()
                        .code(device.getDeviceTypeId())
                        .displayName(device.getDeviceTypeId())
                        .build();
            }
        }

        DeviceDetailResponse.ManufacturerInfo manufacturerInfo = null;
        if (device.getManufacturerId() != null) {
            final var manufacturerOpt = masterDataService.getManufacturerByCode(device.getManufacturerId());
            if (manufacturerOpt.isPresent()) {
                final var manufacturer = manufacturerOpt.get();
                manufacturerInfo = DeviceDetailResponse.ManufacturerInfo.builder()
                        .code(manufacturer.getCode())
                        .name(manufacturer.getName())
                        .build();
            } else {
                // Fallback if master data not found
                manufacturerInfo = DeviceDetailResponse.ManufacturerInfo.builder()
                        .code(device.getManufacturerId())
                        .name(device.getManufacturerId())
                        .build();
            }
        }

        // Build basic details
        final DeviceDetailResponse.BasicDetails basicDetails = DeviceDetailResponse.BasicDetails.builder()
                .name(device.getName())
                .description(device.getDescription())
                .deviceType(deviceTypeInfo)
                .manufacturer(manufacturerInfo)
                .owner(device.getOwner())
                .manufacturedDate(device.getManufacturedDate())
                .companyOwned(device.getCompanyOwned())
                .requiresOperator(device.getRequiresOperator())
                .build();

        // Build media info with fresh presigned URLs (on-demand generation)
        // BUSINESS DECISION: Generate fresh presigned URLs on-demand to ensure they never expire
        // URLs are regenerated for each request, so they're always valid for 7 days from request time
        final List<String> refreshedMediaUrls = mediaUploadService.refreshMediaUrls(
                device.getMediaUrls() != null ? device.getMediaUrls() : new ArrayList<>());
        final String refreshedPrimaryUrl = device.getPrimaryMediaUrl() != null
                ? mediaUploadService.refreshMediaUrl(device.getPrimaryMediaUrl())
                : null;

        final DeviceDetailResponse.MediaInfo mediaInfo = DeviceDetailResponse.MediaInfo.builder()
                .count(refreshedMediaUrls.size())
                .primaryUrl(refreshedPrimaryUrl)
                .allUrls(refreshedMediaUrls)
                .build();

        // Build location info (address and pincode only, NO coordinates)
        final DeviceDetailResponse.LocationInfo locationInfo = DeviceDetailResponse.LocationInfo.builder()
                .address(device.getAddress())
                .pincode(device.getPincode())
                .build();

        // Get pricing information
        PricingRule defaultRule = null;
        List<PricingRule> timeSpecificRules = new ArrayList<>();
        if (device.getDeviceTypeId() != null && device.getPincode() != null) {
            defaultRule = pricingService.getDefaultRule(device.getDeviceTypeId(), device.getPincode());
            timeSpecificRules = pricingService.getTimeSpecificRules(device.getDeviceTypeId(), device.getPincode(), LocalDate.now());
        }

        final DeviceDetailResponse.PricingInfo pricingInfo = DeviceDetailResponse.PricingInfo.builder()
                .defaultRule(defaultRule)
                .timeSpecificRules(timeSpecificRules)
                .build();

        // Build lease info
        final DeviceDetailResponse.LeaseInfo leaseInfo = DeviceDetailResponse.LeaseInfo.builder()
                .leaseState(device.getCurrentLeaseId() != null ? "LEASED" : "AVAILABLE")
                .currentLeaseId(device.getCurrentLeaseId())
                .build();

        // Calculate distance if user location provided
        DeviceDetailResponse.DistanceInfo distanceInfo = null;
        if (userLat != null && userLng != null && device.getLocation() != null) {
            final double distanceKm = calculateDistance(
                    userLng, userLat,
                    device.getLocation().getX(), device.getLocation().getY()
            );
            distanceInfo = DeviceDetailResponse.DistanceInfo.builder()
                    .distanceKm(distanceKm)
                    .build();
        }

        // Build operational info
        final DeviceDetailResponse.OperationalInfo operationalInfo = DeviceDetailResponse.OperationalInfo.builder()
                .operationalState(device.getOperationalState())
                .build();

        // Build comprehensive response
        return DeviceDetailResponse.builder()
                .deviceId(device.getId())
                .basicDetails(basicDetails)
                .media(mediaInfo)
                .location(locationInfo)
                .pricing(pricingInfo)
                .lease(leaseInfo)
                .distance(distanceInfo)
                .operational(operationalInfo)
                .build();
    }

}

