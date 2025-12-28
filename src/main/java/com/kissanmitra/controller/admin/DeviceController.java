package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.Device;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.DeviceRepository;
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

import java.util.List;

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
     * Gets all devices.
     *
     * @return list of devices
     */
    @GetMapping
    public BaseClientResponse<List<Device>> getAllDevices() {
        final List<Device> devices = deviceRepository.findAll();
        return Response.SUCCESS.buildSuccess(generateRequestId(), devices);
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

        final Device updated = builder.build();
        updated.setId(id); // Ensure ID is set
        final Device saved = deviceRepository.save(updated);
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }
}

