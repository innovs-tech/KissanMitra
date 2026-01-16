package com.kissanmitra.controller;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.LeaseService;
import com.kissanmitra.service.VleProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Controller for lease operations (non-admin).
 */
@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;
    private final VleProfileService vleProfileService;
    private final UserContext userContext;

    /**
     * Gets lease by ID.
     *
     * @param id lease ID
     * @return lease
     */
    @GetMapping("/{id}")
    public BaseClientResponse<Lease> getLease(@PathVariable final String id) {
        final Lease lease = leaseService.getLeaseById(id);
        return Response.SUCCESS.buildSuccess(generateRequestId(), lease);
    }

    /**
     * Gets current user's leases (if VLE).
     *
     * <p>Business Context:
     * - Returns leases for the current authenticated user's VLE profile
     * - User must have a VLE profile to access this endpoint
     *
     * <p>Uber Logic:
     * - Gets current user ID from security context
     * - Fetches VLE profile by user ID
     * - Returns leases for that VLE
     *
     * @return list of leases
     */
    @GetMapping("/me")
    public BaseClientResponse<List<Lease>> getMyLeases() {
        final String userId = userContext.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        final VleProfile vleProfile = vleProfileService.getVleProfileByUserId(userId);
        if (vleProfile == null) {
            throw new RuntimeException("VLE profile not found for current user");
        }

        final List<Lease> leases = leaseService.getLeasesByVleId(vleProfile.getId());
        return Response.SUCCESS.buildSuccess(generateRequestId(), leases);
    }
}

