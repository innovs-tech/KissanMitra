package com.kissanmitra.controller.discovery;

import com.kissanmitra.enums.Response;
import com.kissanmitra.request.DiscoverySearchRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.response.DeviceDetailResponse;
import com.kissanmitra.response.DiscoveryResponse;
import com.kissanmitra.service.DiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Public controller for discovery operations.
 *
 * <p>Business Context:
 * - Discovery is public and unauthenticated
 * - Allows users to browse equipment before login
 * - Intent capture for pre-login interest
 *
 * <p>Uber Logic:
 * - No authentication required
 * - Location-based geospatial search
 * - Intent creation with TTL
 */
@RestController
@RequestMapping("/api/v1/public/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * Searches for devices based on location and filters.
     *
     * @param request discovery search request
     * @return discovery response
     */
    @PostMapping("/search")
    public BaseClientResponse<DiscoveryResponse> searchDevices(@Valid @RequestBody final DiscoverySearchRequest request) {
        final DiscoveryResponse response = discoveryService.searchDevices(request);
        return Response.SUCCESS.buildSuccess(generateRequestId(), response);
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
    @PostMapping("/intents")
    public BaseClientResponse<String> createIntent(
            @RequestParam final String deviceId,
            @RequestParam final String intentType,
            @RequestParam(required = false) final Double requestedHours,
            @RequestParam(required = false) final Double requestedAcres
    ) {
        final String intentId = discoveryService.createIntent(deviceId, intentType, requestedHours, requestedAcres);
        return Response.SUCCESS.buildSuccess(generateRequestId(), intentId);
    }

    /**
     * Gets comprehensive device details for detail view.
     *
     * <p>Business Context:
     * - Public endpoint for viewing device details when user selects device from discovery
     * - Only LIVE devices visible to public (ONBOARDED visible to authenticated users)
     * - Returns all device information including media, pricing, location, etc.
     *
     * <p>Uber Logic:
     * - Fetches device by ID
     * - Validates device status
     * - Enriches with master data (deviceType displayName, manufacturer name)
     * - Includes full pricing rules and media information
     * - Calculates distance if user location provided
     *
     * @param deviceId device ID
     * @param lat user's latitude (optional, for distance calculation)
     * @param lng user's longitude (optional, for distance calculation)
     * @return device detail response
     */
    @GetMapping("/devices/{deviceId}")
    public BaseClientResponse<DeviceDetailResponse> getDeviceDetails(
            @PathVariable final String deviceId,
            @RequestParam(required = false) final Double lat,
            @RequestParam(required = false) final Double lng
    ) {
        final DeviceDetailResponse response = discoveryService.getDeviceDetails(deviceId, lat, lng);
        return Response.SUCCESS.buildSuccess(generateRequestId(), response);
    }
}

