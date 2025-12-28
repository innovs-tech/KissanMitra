package com.kissanmitra.service;

import com.kissanmitra.request.DiscoverySearchRequest;
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
}

