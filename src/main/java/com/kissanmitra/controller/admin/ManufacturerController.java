package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.ManufacturerRepository;
import com.kissanmitra.request.CreateManufacturerRequest;
import com.kissanmitra.request.UpdateManufacturerRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.MasterDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for manufacturer master data management.
 *
 * <p>Business Context:
 * - Manufacturers are master data (MAHINDRA, JOHN_DEERE, etc.)
 * - Code is immutable identifier, name can be updated
 * - Soft delete (set active=false) to prevent breaking references
 *
 * <p>Uber Logic:
 * - Code must be unique and uppercase
 * - Cannot delete if devices reference it
 * - Code cannot be changed after creation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturerRepository manufacturerRepository;
    private final DeviceRepository deviceRepository;
    private final MasterDataService masterDataService;

    /**
     * Creates a new manufacturer.
     *
     * <p>Business Decision:
     * - Code must be unique
     * - Active defaults to true if not provided
     *
     * @param request manufacturer creation request
     * @return created manufacturer
     */
    @PostMapping
    public BaseClientResponse<Manufacturer> createManufacturer(@Valid @RequestBody final CreateManufacturerRequest request) {
        // Check if code already exists
        if (manufacturerRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Manufacturer with code already exists: " + request.getCode());
        }

        final Manufacturer manufacturer = Manufacturer.builder()
                .code(request.getCode().toUpperCase()) // BUSINESS DECISION: Normalize to uppercase
                .name(request.getName())
                .active(request.getActive() != null ? request.getActive() : true) // Default to active
                .build();

        final Manufacturer created = manufacturerRepository.save(manufacturer);
        // BUSINESS DECISION: Evict cache after creation to ensure subsequent lookups get fresh data
        masterDataService.evictManufacturerCache(created.getCode());
        log.info("Created manufacturer: {} ({})", created.getCode(), created.getName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Gets all manufacturers (with optional active filter).
     *
     * @param active optional filter by active status
     * @return list of manufacturers
     */
    @GetMapping
    public BaseClientResponse<List<Manufacturer>> getAllManufacturers(
            @RequestParam(required = false) final Boolean active
    ) {
        final List<Manufacturer> manufacturers;
        if (active != null) {
            manufacturers = manufacturerRepository.findByActive(active);
        } else {
            manufacturers = manufacturerRepository.findAll();
        }
        return Response.SUCCESS.buildSuccess(generateRequestId(), manufacturers);
    }

    /**
     * Gets manufacturer by code.
     *
     * @param code manufacturer code
     * @return manufacturer
     */
    @GetMapping("/{code}")
    public BaseClientResponse<Manufacturer> getManufacturerByCode(@PathVariable final String code) {
        final Manufacturer manufacturer = manufacturerRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + code));
        return Response.SUCCESS.buildSuccess(generateRequestId(), manufacturer);
    }

    /**
     * Updates a manufacturer.
     *
     * <p>Business Decision:
     * - Code is immutable (cannot be changed)
     * - Only name and active can be updated
     *
     * @param code manufacturer code
     * @param request update request
     * @return updated manufacturer
     */
    @PutMapping("/{code}")
    public BaseClientResponse<Manufacturer> updateManufacturer(
            @PathVariable final String code,
            @Valid @RequestBody final UpdateManufacturerRequest request
    ) {
        final Manufacturer existing = manufacturerRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + code));

        // BUSINESS DECISION: Code is immutable - cannot be changed
        // Request should not include code, but if it does, we ignore it

        final Manufacturer updated = existing.toBuilder()
                .name(request.getName())
                .active(request.getActive())
                .build();
        updated.setId(existing.getId());

        final Manufacturer saved = manufacturerRepository.save(updated);
        // BUSINESS DECISION: Evict cache after update to ensure subsequent lookups get fresh data
        masterDataService.evictManufacturerCache(saved.getCode());
        log.info("Updated manufacturer: {} ({})", saved.getCode(), saved.getName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), saved);
    }

    /**
     * Soft deletes a manufacturer.
     *
     * <p>Business Decision:
     * - Soft delete (set active=false) instead of hard delete
     * - Prevents deletion if devices reference it
     *
     * @param code manufacturer code
     * @return success response
     */
    @DeleteMapping("/{code}")
    public BaseClientResponse<String> deleteManufacturer(@PathVariable final String code) {
        final Manufacturer manufacturer = manufacturerRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + code));

        // BUSINESS DECISION: Check for references before soft delete
        final long deviceCount = deviceRepository.countByManufacturerId(code.toUpperCase());
        if (deviceCount > 0) {
            throw new RuntimeException(
                    String.format("Cannot delete manufacturer: %d device(s) reference this manufacturer", deviceCount)
            );
        }

        // BUSINESS DECISION: Soft delete (set active=false)
        final Manufacturer updated = manufacturer.toBuilder()
                .active(false)
                .build();
        updated.setId(manufacturer.getId());
        manufacturerRepository.save(updated);
        // BUSINESS DECISION: Evict cache after soft delete to ensure subsequent lookups get fresh data
        masterDataService.evictManufacturerCache(manufacturer.getCode());

        log.info("Soft deleted manufacturer: {} ({})", manufacturer.getCode(), manufacturer.getName());
        return Response.SUCCESS.buildSuccess(generateRequestId(), "Manufacturer soft deleted successfully");
    }
}

