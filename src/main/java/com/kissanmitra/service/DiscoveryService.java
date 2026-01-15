package com.kissanmitra.service;

import com.kissanmitra.request.DiscoverySearchRequest;
import com.kissanmitra.response.DeviceDetailResponse;
import com.kissanmitra.response.DiscoveryResponse;

/**
 * Service interface for discovery operations.
 */
public interface DiscoveryService {

    /**
     * Searches for devices based on location and filters.
     *
     * @param request discovery search request
     * @return discovery response with device results
     */
    DiscoveryResponse searchDevices(DiscoverySearchRequest request);

    /**
     * Creates a discovery intent for pre-login interest.
     *
     * @param deviceId device ID
     * @param intentType intent type (RENT/LEASE)
     * @param requestedHours requested hours
     * @param requestedAcres requested acres
     * @return intent ID
     */
    String createIntent(String deviceId, String intentType, Double requestedHours, Double requestedAcres);

    /**
     * Gets comprehensive device details for detail view.
     *
     * <p>Business Context:
     * - Public endpoint for viewing device details
     * - Only LIVE devices visible to public (ONBOARDED visible to authenticated users)
     * - Enriches deviceType and manufacturer with display names from master data
     * - Includes full pricing rules and media information
     *
     * @param deviceId device ID
     * @param userLat user's latitude (optional, for distance calculation)
     * @param userLng user's longitude (optional, for distance calculation)
     * @return device detail response
     */
    DeviceDetailResponse getDeviceDetails(String deviceId, Double userLat, Double userLng);
}

