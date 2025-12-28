package com.kissanmitra.controller;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.LeaseService;
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
     * @return list of leases
     */
    @GetMapping("/me")
    public BaseClientResponse<List<Lease>> getMyLeases() {
        // TODO: Get VLE ID from current user's VLE profile
        final String vleId = "vle"; // Placeholder
        final List<Lease> leases = leaseService.getLeasesByVleId(vleId);
        return Response.SUCCESS.buildSuccess(generateRequestId(), leases);
    }
}

