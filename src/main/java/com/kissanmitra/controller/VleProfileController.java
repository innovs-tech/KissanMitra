package com.kissanmitra.controller;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.VleProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Controller for VLE profile operations (non-admin).
 */
@RestController
@RequestMapping("/api/v1/vles")
@RequiredArgsConstructor
public class VleProfileController {

    private final VleProfileService vleProfileService;
    private final UserContext userContext;

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
     * Gets current user's VLE profile.
     *
     * @return VLE profile
     */
    @GetMapping("/me")
    public BaseClientResponse<VleProfile> getMyVleProfile() {
        final String userId = userContext.getCurrentUserId();
        final VleProfile vleProfile = vleProfileService.getVleProfileByUserId(userId);
        if (vleProfile == null) {
            throw new RuntimeException("VLE profile not found for current user");
        }
        return Response.SUCCESS.buildSuccess(generateRequestId(), vleProfile);
    }
}

