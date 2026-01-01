package com.kissanmitra.request;

import com.kissanmitra.dto.PricingRuleItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating pricing rules for a device.
 *
 * <p>Business Context:
 * - Admin must configure at least default rule
 * - Time-specific rules are optional
 * - Default rule: effectiveTo = null (ongoing)
 * - Time-specific rules: date range (e.g., peak season pricing)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DevicePricingRuleRequest {

    /**
     * Default pricing rules (required).
     * These rules are always active (effectiveTo = null).
     */
    @NotEmpty(message = "Default rules are required")
    @Valid
    private List<PricingRuleItem> defaultRules;

    /**
     * Time-specific pricing rules (optional).
     * These rules apply only during the specified date range.
     */
    @Valid
    private List<TimeSpecificRule> timeSpecificRules;

    /**
     * Time-specific pricing rule with date range.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TimeSpecificRule {
        /**
         * Start date of the rule (required).
         */
        @NotNull(message = "Effective from date is required")
        private LocalDate effectiveFrom;

        /**
         * End date of the rule (required).
         */
        @NotNull(message = "Effective to date is required")
        private LocalDate effectiveTo;

        /**
         * Pricing rules for this time period (required).
         */
        @NotEmpty(message = "Rules are required")
        @Valid
        private List<PricingRuleItem> rules;
    }
}

