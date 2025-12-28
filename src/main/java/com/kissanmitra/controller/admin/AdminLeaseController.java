package com.kissanmitra.controller.admin;

import com.kissanmitra.dto.OperatorAssignment;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for lease management.
 *
 * <p>Business Context:
 * - Only Admin can create and manage leases
 * - Leases are created from approved LEASE orders
 * - Operators are assigned to leases
 */
@RestController
@RequestMapping("/api/v1/admin/leases")
@RequiredArgsConstructor
@org.springframework.context.annotation.ComponentScan
public class AdminLeaseController {

    private final LeaseService leaseService;

    /**
     * Creates a lease from an approved order.
     *
     * @param orderId approved order ID
     * @return created lease
     */
    @PostMapping
    public BaseClientResponse<Lease> createLease(@RequestParam final String orderId) {
        final Lease lease = leaseService.createLeaseFromOrder(orderId);
        return Response.SUCCESS.buildSuccess(generateRequestId(), lease);
    }

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
     * Gets leases for a VLE.
     *
     * @param vleId VLE ID
     * @return list of leases
     */
    @GetMapping
    public BaseClientResponse<List<Lease>> getLeases(@RequestParam(required = false) final String vleId) {
        final List<Lease> leases = vleId != null
                ? leaseService.getLeasesByVleId(vleId)
                : List.of();
        return Response.SUCCESS.buildSuccess(generateRequestId(), leases);
    }

    /**
     * Assigns an operator to a lease.
     *
     * @param leaseId lease ID
     * @param assignment operator assignment
     * @return updated lease
     */
    @PostMapping("/{leaseId}/operators")
    public BaseClientResponse<Lease> assignOperator(
            @PathVariable final String leaseId,
            @Valid @RequestBody final OperatorAssignment assignment
    ) {
        final Lease lease = leaseService.assignOperator(leaseId, assignment);
        return Response.SUCCESS.buildSuccess(generateRequestId(), lease);
    }
}

