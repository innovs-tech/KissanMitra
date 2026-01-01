package com.kissanmitra.domain.enums;

/**
 * Represents the operational status of a device.
 *
 * <p>Business Context:
 * - Devices go through onboarding flow (DRAFT → ONBOARDED → LIVE)
 * - Only LIVE devices with active pricing rules appear in discovery
 * - Status transitions are managed by Admin
 *
 * <p>Uber Logic:
 * - DRAFT: Onboarding in progress (Steps 1-3 incomplete)
 * - ONBOARDED: All steps complete, hidden from discovery
 * - LIVE: Visible in discovery (requires default pricing rule)
 * - NOT_LIVE/UNDER_MAINTENANCE: Hidden from discovery (admin action)
 * - RETIRED: Permanently removed (cannot be reversed)
 */
public enum DeviceStatus {
    /**
     * Device onboarding in progress (only few steps completed).
     * Device is being set up by admin.
     */
    DRAFT,

    /**
     * All steps completed but not live (hidden from discovery).
     * Device is fully onboarded but admin chose to keep it hidden.
     */
    ONBOARDED,

    /**
     * Available for discovery and leasing (visible to VLEs).
     * Requires active default pricing rule to be LIVE.
     * Only LIVE devices with pricing rules appear in discovery.
     */
    LIVE,

    /**
     * Admin manually hides from discovery.
     * Device is available but not shown to VLEs.
     */
    NOT_LIVE,

    /**
     * Device needs maintenance (hidden from discovery).
     * Device is temporarily unavailable for leasing.
     */
    UNDER_MAINTENANCE,

    /**
     * Device permanently removed from service.
     * Cannot be leased or discovered.
     * Cannot be reversed.
     */
    RETIRED
}

