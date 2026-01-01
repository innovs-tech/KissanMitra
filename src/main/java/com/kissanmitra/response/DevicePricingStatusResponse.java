package com.kissanmitra.response;

import com.kissanmitra.entity.PricingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for device pricing status.
 *
 * <p>Business Context:
 * - Default rule is always required (hasDefaultRule is always true, omitted from response)
 * - Time-specific rules are optional
 * - Used in Step 3 of onboarding to show existing pricing or prompt creation
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DevicePricingStatusResponse {

    /**
     * Default pricing rule (always present if exists).
     * effectiveTo = null (ongoing).
     */
    private PricingRule defaultRule;

    /**
     * Time-specific pricing rules (optional, may be empty).
     * Rules with date ranges.
     */
    private List<PricingRule> timeSpecificRules;

    /**
     * Whether pricing rule creation is required.
     * true if no default rule exists, false otherwise.
     */
    private boolean requiresPricingRule;
}

