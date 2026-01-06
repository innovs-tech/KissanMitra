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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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
     * Gets all pricing rules with optional filters.
     *
     * <p>Business Context:
     * - Admin can filter pricing rules by deviceTypeId, pincode, and status
     * - Supports pagination for large result sets
     * - Returns all pricing rules if no filters provided
     *
     * @param deviceTypeId optional device type filter
     * @param pincode optional pincode filter
     * @param status optional status filter (ACTIVE, INACTIVE)
     * @param page page number (0-based, default 0)
     * @param size page size (default 20)
     * @return list of pricing rules
     */
    @GetMapping("/rules")
    public BaseClientResponse<List<PricingRule>> getAllPricingRules(
            @RequestParam(value = "deviceTypeId", required = false) final String deviceTypeId,
            @RequestParam(value = "pincode", required = false) final String pincode,
            @RequestParam(value = "status", required = false) final String status,
            @RequestParam(value = "page", defaultValue = "0") final int page,
            @RequestParam(value = "size", defaultValue = "20") final int size
    ) {
        List<PricingRule> rules = pricingRuleRepository.findAll();

        // Filter by deviceTypeId if provided
        if (deviceTypeId != null && !deviceTypeId.isEmpty()) {
            rules = rules.stream()
                    .filter(r -> deviceTypeId.equalsIgnoreCase(r.getDeviceTypeId()))
                    .collect(Collectors.toList());
        }

        // Filter by pincode if provided
        if (pincode != null && !pincode.isEmpty()) {
            rules = rules.stream()
                    .filter(r -> pincode.equals(r.getPincode()))
                    .collect(Collectors.toList());
        }

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            rules = rules.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());
        }

        // Apply pagination
        final List<PricingRule> pagedRules;
        if (rules.isEmpty()) {
            pagedRules = List.of();
        } else {
            final int start = page * size;
            final int end = Math.min(start + size, rules.size());
            pagedRules = rules.subList(Math.min(start, rules.size()), end);
        }

        return Response.SUCCESS.buildSuccess(generateRequestId(), pagedRules);
    }

    /**
     * Gets pricing rule by ID.
     *
     * @param id pricing rule ID
     * @return pricing rule
     */
    @GetMapping("/rules/{id}")
    public BaseClientResponse<PricingRule> getPricingRuleById(@PathVariable final String id) {
        final PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pricing rule not found: " + id));
        return Response.SUCCESS.buildSuccess(generateRequestId(), rule);
    }

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

