package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.VleProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for VLE profile management.
 *
 * <p>Business Context:
 * - Only Admin can create and manage VLE profiles
 * - VLE profiles link users to business entities
 */
@RestController
@RequestMapping("/api/v1/admin/vles")
@RequiredArgsConstructor
public class VleController {

    private final VleProfileService vleProfileService;

    /**
     * Creates a new VLE profile.
     *
     * @param vleProfile VLE profile to create
     * @return created VLE profile
     */
    @PostMapping
    public BaseClientResponse<VleProfile> createVleProfile(@Valid @RequestBody final VleProfile vleProfile) {
        final VleProfile created = vleProfileService.createVleProfile(vleProfile);
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Gets VLE profile by ID.
     *
     * @param id VLE profile ID
     * @return VLE profile
     */
    @GetMapping("/{id}")
    public BaseClientResponse<VleProfile> getVleProfile(@PathVariable final String id) {
        final VleProfile vleProfile = vleProfileService.getVleProfileById(id);
        return Response.SUCCESS.buildSuccess(generateRequestId(), vleProfile);
    }

    /**
     * Gets all VLE profiles.
     *
     * @return list of VLE profiles
     */
    @GetMapping
    public BaseClientResponse<List<VleProfile>> getAllVleProfiles() {
        final List<VleProfile> vleProfiles = vleProfileService.getAllVleProfiles();
        return Response.SUCCESS.buildSuccess(generateRequestId(), vleProfiles);
    }
}

