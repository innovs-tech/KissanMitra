package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.ThresholdConfig;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.response.BaseClientResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for pricing and threshold management.
 */
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingRuleRepository pricingRuleRepository;
    private final ThresholdConfigRepository thresholdConfigRepository;

    /**
     * Creates a new pricing rule.
     *
     * @param pricingRule pricing rule to create
     * @return created pricing rule
     */
    @PostMapping("/rules")
    public BaseClientResponse<PricingRule> createPricingRule(@Valid @RequestBody final PricingRule pricingRule) {
        final PricingRule created = pricingRuleRepository.save(pricingRule);
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Updates a pricing rule.
     *
     * @param id pricing rule ID
     * @param pricingRule pricing rule to update
     * @return updated pricing rule
     */
    @PutMapping("/rules/{id}")
    public BaseClientResponse<PricingRule> updatePricingRule(
            @PathVariable final String id,
            @Valid @RequestBody final PricingRule pricingRule
    ) {
        pricingRule.setId(id);
        final PricingRule updated = pricingRuleRepository.save(pricingRule);
        return Response.SUCCESS.buildSuccess(generateRequestId(), updated);
    }

    /**
     * Creates or updates threshold config.
     *
     * @param thresholdConfig threshold config
     * @return saved threshold config
     */
    @PostMapping("/thresholds")
    public BaseClientResponse<ThresholdConfig> saveThresholdConfig(@Valid @RequestBody final ThresholdConfig thresholdConfig) {
        final ThresholdConfig saved = thresholdConfigRepository.save(thresholdConfig);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Gets threshold config for device type.
     *
     * @param deviceTypeId device type ID
     * @return threshold config
     */
    @GetMapping("/thresholds/{deviceTypeId}")
    public BaseClientResponse<ThresholdConfig> getThresholdConfig(@PathVariable final String deviceTypeId) {
        final ThresholdConfig config = thresholdConfigRepository
                .findByDeviceTypeIdAndStatus(deviceTypeId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Threshold config not found"));
        return Response.SUCCESS.buildSuccess(generateRequestId(), config);
    }
}

