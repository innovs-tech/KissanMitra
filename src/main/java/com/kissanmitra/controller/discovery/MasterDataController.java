package com.kissanmitra.controller.discovery;

import com.kissanmitra.entity.DeviceType;
import com.kissanmitra.entity.Manufacturer;
import com.kissanmitra.enums.Response;
import com.kissanmitra.repository.DeviceTypeRepository;
import com.kissanmitra.repository.ManufacturerRepository;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.response.MasterDataItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Public controller for master data dropdowns.
 *
 * <p>Business Context:
 * - Public endpoints (no authentication required)
 * - Returns only active items for frontend dropdowns
 * - Used in device onboarding and discovery filters
 *
 * <p>Uber Logic:
 * - Returns code + displayName pairs
 * - Only active items are included
 * - Lightweight response for dropdown population
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class MasterDataController {

    private final DeviceTypeRepository deviceTypeRepository;
    private final ManufacturerRepository manufacturerRepository;

    /**
     * Gets active device types for dropdown.
     *
     * @return list of active device types (code + displayName)
     */
    @GetMapping("/device-types")
    public BaseClientResponse<List<MasterDataItemResponse>> getActiveDeviceTypes() {
        final List<DeviceType> deviceTypes = deviceTypeRepository.findByActive(true);
        final List<MasterDataItemResponse> items = deviceTypes.stream()
                .map(dt -> MasterDataItemResponse.builder()
                        .code(dt.getCode())
                        .displayName(dt.getDisplayName())
                        .active(dt.getActive())
                        .build())
                .collect(Collectors.toList());
        return Response.SUCCESS.buildSuccess(generateRequestId(), items);
    }

    /**
     * Gets active manufacturers for dropdown.
     *
     * @return list of active manufacturers (code + name)
     */
    @GetMapping("/manufacturers")
    public BaseClientResponse<List<MasterDataItemResponse>> getActiveManufacturers() {
        final List<Manufacturer> manufacturers = manufacturerRepository.findByActive(true);
        final List<MasterDataItemResponse> items = manufacturers.stream()
                .map(m -> MasterDataItemResponse.builder()
                        .code(m.getCode())
                        .displayName(m.getName()) // BUSINESS DECISION: Use name as displayName for Manufacturer
                        .active(m.getActive())
                        .build())
                .collect(Collectors.toList());
        return Response.SUCCESS.buildSuccess(generateRequestId(), items);
    }
}

