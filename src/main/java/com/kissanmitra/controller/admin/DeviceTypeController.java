package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.DeviceTypeRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.repository.ThresholdConfigRepository;
import com.kissanmitra.request.CreateDeviceTypeRequest;
import com.kissanmitra.request.UpdateDeviceTypeRequest;
import com.kissanmitra.response.BaseClientResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for device type master data management.
 *
 * <p>Business Context:
 * - Device types are master data (TRACTOR, HARVESTER, etc.)
 * - Code is immutable identifier, displayName can be updated
 * - Soft delete (set active=false) to prevent breaking references
 *
 * <p>Uber Logic:
 * - Code must be unique and uppercase
 * - Cannot delete if devices/pricing rules reference it
 * - Code cannot be changed after creation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceRepository deviceRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final ThresholdConfigRepository thresholdConfigRepository;

    /**
     * Creates a new device type.
     *
     * <p>Business Decision:
     * - Code must be unique
     * - Active defaults to true if not provided
     *
     * @param request device type creation request
     * @return created device type
     */
    @PostMapping
    public BaseClientResponse<DeviceType> createDeviceType(@Valid @RequestBody final CreateDeviceTypeRequest request) {
        // Check if code already exists
        if (deviceTypeRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Device type with code already exists: " + request.getCode());
        }

        final DeviceType deviceType = DeviceType.builder()
                .code(request.getCode().toUpperCase()) // BUSINESS DECISION: Normalize to uppercase
                .displayName(request.getDisplayName())
                .requiresOperator(request.getRequiresOperator())
                .active(request.getActive() != null ? request.getActive() : true) // Default to active
                .build();

        final DeviceType created = deviceTypeRepository.save(deviceType);
        log.info("Created device type: {} ({})", created.getCode(), created.getDisplayName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Gets all device types (with optional active filter).
     *
     * @param active optional filter by active status
     * @return list of device types
     */
    @GetMapping
    public BaseClientResponse<List<DeviceType>> getAllDeviceTypes(
            @RequestParam(required = false) final Boolean active
    ) {
        final List<DeviceType> deviceTypes;
        if (active != null) {
            deviceTypes = deviceTypeRepository.findByActive(active);
        } else {
            deviceTypes = deviceTypeRepository.findAll();
        }
        return Response.SUCCESS.buildSuccess(generateRequestId(), deviceTypes);
    }

    /**
     * Gets device type by code.
     *
     * @param code device type code
     * @return device type
     */
    @GetMapping("/{code}")
    public BaseClientResponse<DeviceType> getDeviceTypeByCode(@PathVariable final String code) {
        final DeviceType deviceType = deviceTypeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Device type not found: " + code));
        return Response.SUCCESS.buildSuccess(generateRequestId(), deviceType);
    }

    /**
     * Updates a device type.
     *
     * <p>Business Decision:
     * - Code is immutable (cannot be changed)
     * - Only displayName, requiresOperator, and active can be updated
     *
     * @param code device type code
     * @param request update request
     * @return updated device type
     */
    @PutMapping("/{code}")
    public BaseClientResponse<DeviceType> updateDeviceType(
            @PathVariable final String code,
            @Valid @RequestBody final UpdateDeviceTypeRequest request
    ) {
        final DeviceType existing = deviceTypeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Device type not found: " + code));

        // BUSINESS DECISION: Code is immutable - cannot be changed
        // Request should not include code, but if it does, we ignore it

        final DeviceType updated = existing.toBuilder()
                .displayName(request.getDisplayName())
                .requiresOperator(request.getRequiresOperator())
                .active(request.getActive())
                .build();
        updated.setId(existing.getId());

        final DeviceType saved = deviceTypeRepository.save(updated);
        log.info("Updated device type: {} ({})", saved.getCode(), saved.getDisplayName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Soft deletes a device type.
     *
     * <p>Business Decision:
     * - Soft delete (set active=false) instead of hard delete
     * - Prevents deletion if devices or pricing rules reference it
     *
     * @param code device type code
     * @return success response
     */
    @DeleteMapping("/{code}")
    public BaseClientResponse<String> deleteDeviceType(@PathVariable final String code) {
        final DeviceType deviceType = deviceTypeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Device type not found: " + code));

        // BUSINESS DECISION: Check for references before soft delete
        final long deviceCount = deviceRepository.countByDeviceTypeId(code.toUpperCase());
        if (deviceCount > 0) {
            throw new RuntimeException(
                    String.format("Cannot delete device type: %d device(s) reference this type", deviceCount)
            );
        }

        final long pricingRuleCount = pricingRuleRepository.countByDeviceTypeId(code.toUpperCase());
        if (pricingRuleCount > 0) {
            throw new RuntimeException(
                    String.format("Cannot delete device type: %d pricing rule(s) reference this type", pricingRuleCount)
            );
        }

        final long thresholdCount = thresholdConfigRepository.countByDeviceTypeId(code.toUpperCase());
        if (thresholdCount > 0) {
            throw new RuntimeException(
                    String.format("Cannot delete device type: %d threshold config(s) reference this type", thresholdCount)
            );
        }

        // BUSINESS DECISION: Soft delete (set active=false)
        final DeviceType updated = deviceType.toBuilder()
                .active(false)
                .build();
        updated.setId(deviceType.getId());
        deviceTypeRepository.save(updated);

        log.info("Soft deleted device type: {} ({})", deviceType.getCode(), deviceType.getDisplayName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), "Device type soft deleted successfully");
    }
}

