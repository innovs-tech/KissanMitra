package com.kissanmitra.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for finalizing device onboarding.
 *
 * <p>Business Context:
 * - Step 4 of onboarding flow
 * - Admin can choose to ONBOARD (hidden) or TAKE_LIVE (visible)
 * - TAKE_LIVE requires active default pricing rule
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceFinalizeRequest {

    /**
     * Finalization action.
     * ONBOARD: Set status to ONBOARDED (hidden from discovery)
     * TAKE_LIVE: Set status to LIVE (visible in discovery, requires pricing rule)
     */
    @NotNull(message = "Action is required")
    private FinalizeAction action;

    /**
     * Finalization action enum.
     */
    public enum FinalizeAction {
        /**
         * Onboard device (hidden from discovery).
         * All steps complete but device not visible to VLEs.
         */
        ONBOARD,

        /**
         * Take device live (visible in discovery).
         * Requires active default pricing rule.
         */
        TAKE_LIVE
    }
}

