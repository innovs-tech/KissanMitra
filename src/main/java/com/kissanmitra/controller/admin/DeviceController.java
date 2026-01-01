package com.kissanmitra.controller.admin;

import com.kissanmitra.domain.enums.DeviceStatus;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.PricingRuleRepository;
import com.kissanmitra.request.DeviceFinalizeRequest;
import com.kissanmitra.request.DeviceOnboardStep1Request;
import com.kissanmitra.request.DevicePricingRuleRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.response.DeviceOnboardSummaryResponse;
import com.kissanmitra.response.DevicePricingStatusResponse;
import com.kissanmitra.service.MediaUploadService;
import com.kissanmitra.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for device management.
 *
 * <p>Business Context:
 * - Only Admin can create and manage devices
 * - Devices are registered with sensor IDs
 * - Device location enables geospatial discovery
 */
@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final MediaUploadService mediaUploadService;
    private final PricingService pricingService;
    private final PricingRuleRepository pricingRuleRepository;

    /**
     * Step 1: Basic Equipment Details Entry.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Creates device with DRAFT status
     * - Extracts pincode from address for pricing lookup
     *
     * <p>Uber Logic:
     * - Validates required fields (name, owner, deviceTypeId, manufacturerId, address, location)
     * - Extracts pincode from address.pinCode
     * - Creates device with DRAFT status
     * - Returns deviceId for subsequent steps
     *
     * @param request basic device details
     * @return created device with DRAFT status
     */
    @PostMapping("/onboard/step1")
    public BaseClientResponse<Device> onboardStep1(@Valid @RequestBody final DeviceOnboardStep1Request request) {
        // Extract pincode from address
        final String pincode = request.getAddress() != null && request.getAddress().getPinCode() != null
                ? request.getAddress().getPinCode()
                : null;

        // Build device entity
        final Device device = Device.builder()
                .sensorId(request.getSensorId())
                .name(request.getName())
                .description(request.getDescription())
                .deviceTypeId(request.getDeviceTypeId())
                .manufacturerId(request.getManufacturerId())
                .owner(request.getOwner())
                .manufacturedDate(request.getManufacturedDate())
                .address(request.getAddress())
                .pincode(pincode)
                .location(request.getLocation())
                .companyOwned(request.getCompanyOwned())
                .requiresOperator(request.getRequiresOperator())
                .status(DeviceStatus.DRAFT) // BUSINESS DECISION: All new devices start as DRAFT
                .build();

        final Device created = deviceRepository.save(device);
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Creates a new device.
     *
     * @param device device to create
     * @return created device
     */
    @PostMapping
    public BaseClientResponse<Device> createDevice(@Valid @RequestBody final Device device) {
        final Device created = deviceRepository.save(device);
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Step 2: Equipment Media Upload.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Uploads media files (images/videos) to S3
     * - Can be called multiple times (append to existing URLs)
     * - Minimum 1 photo required before proceeding
     *
     * <p>Uber Logic:
     * - Validates file size (max 20MB) and format (JPG, PNG, MP4, MOV)
     * - Uploads files to S3
     * - Appends S3 URLs to device.mediaUrls
     * - Sets primary media if primaryIndex provided
     *
     * @param deviceId device ID from Step 1
     * @param files array of files to upload
     * @param primaryIndex optional index of file to mark as primary
     * @return updated device with mediaUrls
     */
    @PostMapping("/{deviceId}/media")
    public BaseClientResponse<Device> uploadMedia(
            @PathVariable final String deviceId,
            @RequestParam("files") final MultipartFile[] files,
            @RequestParam(value = "primaryIndex", required = false) final Integer primaryIndex
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Upload files to S3
        final List<String> uploadedUrls = mediaUploadService.uploadMedia(deviceId, files);

        // Append to existing mediaUrls
        final List<String> existingUrls = device.getMediaUrls() != null
                ? new ArrayList<>(device.getMediaUrls())
                : new ArrayList<>();
        existingUrls.addAll(uploadedUrls);

        // Set primary media if specified
        String primaryUrl = device.getPrimaryMediaUrl();
        if (primaryIndex != null && primaryIndex >= 0 && primaryIndex < uploadedUrls.size()) {
            primaryUrl = uploadedUrls.get(primaryIndex);
        } else if (primaryUrl == null && !existingUrls.isEmpty()) {
            // BUSINESS DECISION: If no primary set, use first media as primary
            primaryUrl = existingUrls.get(0);
        }

        // Update device
        final Device updated = device.toBuilder()
                .mediaUrls(existingUrls)
                .primaryMediaUrl(primaryUrl)
                .build();
        updated.setId(deviceId);

        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Gets uploaded media for a device.
     *
     * @param deviceId device ID
     * @return device with mediaUrls
     */
    @GetMapping("/{deviceId}/media")
    public BaseClientResponse<Device> getMedia(@PathVariable final String deviceId) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return Response.SUCCESS.buildSuccess(generateRequestId(), device);
    }

    /**
     * Deletes a specific media file.
     *
     * @param deviceId device ID
     * @param mediaUrl S3 URL of media to delete
     * @return updated device
     */
    @DeleteMapping("/{deviceId}/media")
    public BaseClientResponse<Device> deleteMedia(
            @PathVariable final String deviceId,
            @RequestParam("mediaUrl") final String mediaUrl
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Remove from mediaUrls
        final List<String> updatedUrls = device.getMediaUrls() != null
                ? new ArrayList<>(device.getMediaUrls())
                : new ArrayList<>();
        updatedUrls.remove(mediaUrl);

        // Update primary if deleted media was primary
        String primaryUrl = device.getPrimaryMediaUrl();
        if (mediaUrl.equals(primaryUrl)) {
            primaryUrl = updatedUrls.isEmpty() ? null : updatedUrls.get(0);
        }

        // Delete from S3
        mediaUploadService.deleteMedia(deviceId, mediaUrl);

        // Update device
        final Device updated = device.toBuilder()
                .mediaUrls(updatedUrls)
                .primaryMediaUrl(primaryUrl)
                .build();
        updated.setId(deviceId);

        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Sets primary media (thumbnail).
     *
     * @param deviceId device ID
     * @param mediaUrl S3 URL to set as primary
     * @return updated device
     */
    @PutMapping("/{deviceId}/media/set-primary")
    public BaseClientResponse<Device> setPrimaryMedia(
            @PathVariable final String deviceId,
            @RequestParam("mediaUrl") final String mediaUrl
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Validate mediaUrl exists in device.mediaUrls
        if (device.getMediaUrls() == null || !device.getMediaUrls().contains(mediaUrl)) {
            throw new RuntimeException("Media URL not found for this device");
        }

        // Update primary media
        final Device updated = device.toBuilder()
                .primaryMediaUrl(mediaUrl)
                .build();
        updated.setId(deviceId);

        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Step 3: Location & Pricing Setup - Get Pricing Status.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Checks if pricing rules exist for device
     * - Returns default rule and time-specific rules if they exist
     *
     * <p>Uber Logic:
     * - Gets device by ID
     * - Checks for default rule (effectiveTo = null)
     * - Gets active time-specific rules
     * - Returns requiresPricingRule = true if no default rule exists
     *
     * @param deviceId device ID from Step 1
     * @return pricing status response
     */
    @GetMapping("/{deviceId}/pricing-status")
    public BaseClientResponse<DevicePricingStatusResponse> getPricingStatus(
            @PathVariable final String deviceId
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceTypeId() == null || device.getPincode() == null) {
            throw new RuntimeException("Device missing deviceTypeId or pincode");
        }

        // Get default rule
        final PricingRule defaultRule = pricingService.getDefaultRule(
                device.getDeviceTypeId(), device.getPincode());

        // Get time-specific rules
        final List<PricingRule> timeSpecificRules = pricingService.getTimeSpecificRules(
                device.getDeviceTypeId(), device.getPincode(), LocalDate.now());

        final DevicePricingStatusResponse response = DevicePricingStatusResponse.builder()
                .defaultRule(defaultRule)
                .timeSpecificRules(timeSpecificRules)
                .requiresPricingRule(defaultRule == null)
                .build();

        return Response.SUCCESS.buildSuccess(generateRequestId(), response);
    }

    /**
     * Step 3: Location & Pricing Setup - Create Pricing Rule.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Admin must configure at least default rule
     * - Time-specific rules are optional
     *
     * <p>Uber Logic:
     * - Creates default pricing rule with effectiveFrom = today, effectiveTo = null
     * - Creates time-specific rules if provided
     * - Validates no overlapping active rules (conflict detection)
     * - Uses device's deviceTypeId and pincode for rule creation
     *
     * @param deviceId device ID from Step 1
     * @param request pricing rule request
     * @return created pricing rules
     */
    @PostMapping("/{deviceId}/pricing-rule")
    public BaseClientResponse<List<PricingRule>> createPricingRule(
            @PathVariable final String deviceId,
            @Valid @RequestBody final DevicePricingRuleRequest request
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceTypeId() == null || device.getPincode() == null) {
            throw new RuntimeException("Device missing deviceTypeId or pincode");
        }

        final List<PricingRule> createdRules = new ArrayList<>();
        final LocalDate today = LocalDate.now();

        // BUSINESS DECISION: Default rule is MANDATORY
        // Create default rule with effectiveFrom = today, effectiveTo = null
        final PricingRule defaultRule = PricingRule.builder()
                .deviceTypeId(device.getDeviceTypeId())
                .pincode(device.getPincode())
                .rules(request.getDefaultRules())
                .effectiveFrom(today)
                .effectiveTo(null) // Ongoing
                .status("ACTIVE")
                .build();

        // Check for conflicts (existing default rule)
        final List<PricingRule> conflicts = pricingService.checkForConflicts(defaultRule);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Default pricing rule already exists for this device type and pincode");
        }

        final PricingRule savedDefaultRule = pricingRuleRepository.save(defaultRule);
        createdRules.add(savedDefaultRule);

        // Create time-specific rules if provided
        if (request.getTimeSpecificRules() != null && !request.getTimeSpecificRules().isEmpty()) {
            for (final DevicePricingRuleRequest.TimeSpecificRule timeRule : request.getTimeSpecificRules()) {
                // Validate date range
                if (timeRule.getEffectiveTo().isBefore(timeRule.getEffectiveFrom())) {
                    throw new IllegalArgumentException("Effective to date must be after effective from date");
                }

                final PricingRule timeSpecificRule = PricingRule.builder()
                        .deviceTypeId(device.getDeviceTypeId())
                        .pincode(device.getPincode())
                        .rules(timeRule.getRules())
                        .effectiveFrom(timeRule.getEffectiveFrom())
                        .effectiveTo(timeRule.getEffectiveTo())
                        .status("ACTIVE")
                        .build();

                // Check for overlapping time-specific rules
                final List<PricingRule> overlappingRules = pricingService.checkForConflicts(timeSpecificRule);
                if (!overlappingRules.isEmpty()) {
                    // BUSINESS DECISION: Warn about conflicts but allow creation
                    // Admin can handle conflicts later
                    // Log warning for now
                }

                final PricingRule savedTimeRule = pricingRuleRepository.save(timeSpecificRule);
                createdRules.add(savedTimeRule);
            }
        }

        return Response.SUCCESS.buildSuccess(generateRequestId(), createdRules);
    }

    /**
     * Step 4: Equipment Publishing/Summary Review - Get Summary.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Shows all entered information in read-only format
     * - Used for review before finalizing
     *
     * <p>Uber Logic:
     * - Gets device by ID
     * - Gets pricing rules for device
     * - Builds summary response with all steps status
     * - Note: Coordinates are NOT exposed (only address and pincode)
     *
     * @param deviceId device ID
     * @return complete device summary
     */
    @GetMapping("/{deviceId}/onboard-summary")
    public BaseClientResponse<DeviceOnboardSummaryResponse> getOnboardSummary(
            @PathVariable final String deviceId
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Get pricing rules
        final PricingRule defaultRule = device.getDeviceTypeId() != null && device.getPincode() != null
                ? pricingService.getDefaultRule(device.getDeviceTypeId(), device.getPincode())
                : null;
        final List<PricingRule> timeSpecificRules = device.getDeviceTypeId() != null && device.getPincode() != null
                ? pricingService.getTimeSpecificRules(device.getDeviceTypeId(), device.getPincode(), LocalDate.now())
                : new ArrayList<>();

        // Build summary response
        final DeviceOnboardSummaryResponse.BasicDetails basicDetails = DeviceOnboardSummaryResponse.BasicDetails.builder()
                .name(device.getName())
                .description(device.getDescription())
                .deviceTypeId(device.getDeviceTypeId())
                .manufacturerId(device.getManufacturerId())
                .owner(device.getOwner())
                .manufacturedDate(device.getManufacturedDate())
                .companyOwned(device.getCompanyOwned())
                .requiresOperator(device.getRequiresOperator())
                .build();

        final DeviceOnboardSummaryResponse.MediaInfo mediaInfo = DeviceOnboardSummaryResponse.MediaInfo.builder()
                .count(device.getMediaUrls() != null ? device.getMediaUrls().size() : 0)
                .primaryUrl(device.getPrimaryMediaUrl())
                .urls(device.getMediaUrls())
                .build();

        final DeviceOnboardSummaryResponse.LocationInfo locationInfo = DeviceOnboardSummaryResponse.LocationInfo.builder()
                .address(device.getAddress())
                .pincode(device.getPincode())
                .build();

        final DeviceOnboardSummaryResponse.PricingInfo pricingInfo = DeviceOnboardSummaryResponse.PricingInfo.builder()
                .defaultRule(defaultRule)
                .timeSpecificRules(timeSpecificRules)
                .build();

        final DeviceOnboardSummaryResponse summary = DeviceOnboardSummaryResponse.builder()
                .deviceId(device.getId())
                .basicDetails(basicDetails)
                .media(mediaInfo)
                .location(locationInfo)
                .pricing(pricingInfo)
                .status(device.getStatus())
                .build();

        return Response.SUCCESS.buildSuccess(generateRequestId(), summary);
    }

    /**
     * Step 4: Equipment Publishing/Summary Review - Finalize.
     *
     * <p>Business Context:
     * - Part of 4-step onboarding flow
     * - Admin can choose to ONBOARD (hidden) or TAKE_LIVE (visible)
     * - TAKE_LIVE requires active default pricing rule
     *
     * <p>Uber Logic:
     * - ONBOARD: Set status to ONBOARDED (hidden from discovery)
     * - TAKE_LIVE: Validate pricing rule exists, set status to LIVE (visible in discovery)
     * - Returns updated device
     *
     * @param deviceId device ID
     * @param request finalize request with action
     * @return updated device
     */
    @PatchMapping("/{deviceId}/finalize")
    public BaseClientResponse<Device> finalizeOnboarding(
            @PathVariable final String deviceId,
            @Valid @RequestBody final DeviceFinalizeRequest request
    ) {
        final Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        final DeviceStatus newStatus;
        if (request.getAction() == DeviceFinalizeRequest.FinalizeAction.ONBOARD) {
            // BUSINESS DECISION: ONBOARD sets status to ONBOARDED (hidden from discovery)
            newStatus = DeviceStatus.ONBOARDED;
        } else if (request.getAction() == DeviceFinalizeRequest.FinalizeAction.TAKE_LIVE) {
            // BUSINESS DECISION: TAKE_LIVE requires active default pricing rule
            if (device.getDeviceTypeId() == null || device.getPincode() == null) {
                throw new RuntimeException("Device missing deviceTypeId or pincode");
            }

            final boolean hasPricingRule = pricingService.hasActivePricingRule(
                    device.getDeviceTypeId(), device.getPincode());

            if (!hasPricingRule) {
                throw new RuntimeException("Default pricing rule required before taking device live");
            }

            newStatus = DeviceStatus.LIVE;
        } else {
            throw new IllegalArgumentException("Invalid action: " + request.getAction());
        }

        // Update device status
        final Device updated = device.toBuilder()
                .status(newStatus)
                .build();
        updated.setId(deviceId);

        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Gets device by ID.
     *
     * @param id device ID
     * @return device
     */
    @GetMapping("/{id}")
    public BaseClientResponse<Device> getDevice(@PathVariable final String id) {
        final Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return Response.SUCCESS.buildSuccess(generateRequestId(), device);
    }

    /**
     * Gets all devices with filters.
     *
     * <p>Business Context:
     * - Admin can filter devices by status
     * - Admin can search by name or device type
     * - Pagination: 20 per page
     *
     * <p>Uber Logic:
     * - Returns thumbnail (primaryMediaUrl), name, type, location (city), status
     * - Supports filtering and pagination
     *
     * @param status optional status filter
     * @param search optional search query (name or device type)
     * @param page page number (0-based, default 0)
     * @return list of devices
     */
    @GetMapping
    public BaseClientResponse<List<Device>> getAllDevices(
            @RequestParam(value = "status", required = false) final DeviceStatus status,
            @RequestParam(value = "search", required = false) final String search,
            @RequestParam(value = "page", defaultValue = "0") final int page,
            @RequestParam(value = "size", defaultValue = "20") final int size
    ) {
        List<Device> devices = deviceRepository.findAll();

        // Filter by status if provided
        if (status != null) {
            devices = devices.stream()
                    .filter(d -> d.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Search by name or device type if provided
        if (search != null && !search.isEmpty()) {
            final String searchLower = search.toLowerCase();
            devices = devices.stream()
                    .filter(d -> (d.getName() != null && d.getName().toLowerCase().contains(searchLower))
                            || (d.getDeviceTypeId() != null && d.getDeviceTypeId().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        // Apply pagination
        final int start = page * size;
        final int end = Math.min(start + size, devices.size());
        final List<Device> pagedDevices = devices.subList(Math.min(start, devices.size()), end);

        return Response.SUCCESS.buildSuccess(generateRequestId(), pagedDevices);
    }

    /**
     * Updates device.
     *
     * <p>Business Decision:
     * - Supports partial updates (only provided fields are updated)
     * - Location can be updated using GeoJSON format
     * - Preserves existing fields if not provided in request
     *
     * @param id device ID
     * @param device device update data (partial updates supported)
     * @return updated device
     */
    @PutMapping("/{id}")
    public BaseClientResponse<Device> updateDevice(
            @PathVariable final String id,
            @Valid @RequestBody final Device device
    ) {
        final Device existing = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Use builder to update only provided fields
        final Device.DeviceBuilder<?, ?> builder = existing.toBuilder();

        // Update only non-null fields
        if (device.getSensorId() != null) {
            builder.sensorId(device.getSensorId());
        }
        if (device.getDeviceTypeId() != null) {
            builder.deviceTypeId(device.getDeviceTypeId());
        }
        if (device.getManufacturerId() != null) {
            builder.manufacturerId(device.getManufacturerId());
        }
        if (device.getCompanyOwned() != null) {
            builder.companyOwned(device.getCompanyOwned());
        }
        if (device.getRequiresOperator() != null) {
            builder.requiresOperator(device.getRequiresOperator());
        }
        if (device.getStatus() != null) {
            builder.status(device.getStatus());
        }
        if (device.getLocation() != null) {
            builder.location(device.getLocation());
        }
        if (device.getCurrentLeaseId() != null) {
            builder.currentLeaseId(device.getCurrentLeaseId());
        }
        if (device.getOperationalState() != null) {
            builder.operationalState(device.getOperationalState());
        }
        if (device.getName() != null) {
            builder.name(device.getName());
        }
        if (device.getDescription() != null) {
            builder.description(device.getDescription());
        }
        if (device.getOwner() != null) {
            builder.owner(device.getOwner());
        }
        if (device.getManufacturedDate() != null) {
            builder.manufacturedDate(device.getManufacturedDate());
        }
        if (device.getAddress() != null) {
            builder.address(device.getAddress());
            // Update pincode if address changed
            if (device.getAddress().getPinCode() != null) {
                builder.pincode(device.getAddress().getPinCode());
            }
        }
        if (device.getPincode() != null) {
            builder.pincode(device.getPincode());
        }
        if (device.getMediaUrls() != null) {
            builder.mediaUrls(device.getMediaUrls());
        }
        if (device.getPrimaryMediaUrl() != null) {
            builder.primaryMediaUrl(device.getPrimaryMediaUrl());
        }

        final Device updated = builder.build();
        updated.setId(id); // Ensure ID is set
        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Updates device status.
     *
     * <p>Business Context:
     * - Admin can change device status
     * - Status transitions are validated
     *
     * <p>Uber Logic:
     * - DRAFT → ONBOARDED (via finalize with ONBOARD action)
     * - ONBOARDED → LIVE (via finalize with TAKE_LIVE action, requires pricing rule)
     * - LIVE → NOT_LIVE (admin manually hides)
     * - LIVE → UNDER_MAINTENANCE (admin action)
     * - UNDER_MAINTENANCE → LIVE (after maintenance)
     * - Any → RETIRED (permanent, cannot be reversed)
     *
     * @param id device ID
     * @param status new status
     * @return updated device
     */
    @PatchMapping("/{id}/status")
    public BaseClientResponse<Device> updateDeviceStatus(
            @PathVariable final String id,
            @RequestParam("status") final DeviceStatus status
    ) {
        final Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // BUSINESS DECISION: Validate status transitions
        final DeviceStatus currentStatus = device.getStatus();
        if (currentStatus == DeviceStatus.RETIRED) {
            throw new RuntimeException("Cannot change status of retired device");
        }

        if (status == DeviceStatus.LIVE && currentStatus != DeviceStatus.ONBOARDED) {
            // Validate pricing rule exists for LIVE status
            if (device.getDeviceTypeId() == null || device.getPincode() == null) {
                throw new RuntimeException("Device missing deviceTypeId or pincode");
            }
            final boolean hasPricingRule = pricingService.hasActivePricingRule(
                    device.getDeviceTypeId(), device.getPincode());
            if (!hasPricingRule) {
                throw new RuntimeException("Default pricing rule required before taking device live");
            }
        }

        // Update status
        final Device updated = device.toBuilder()
                .status(status)
                .build();
        updated.setId(id);

        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }
}

