package com.kissanmitra.service;

import com.kissanmitra.entity.Lease;
import com.kissanmitra.dto.OperatorAssignment;

import java.util.List;

/**
 * Service interface for lease management.
 */
public interface LeaseService {

    /**
     * Creates a lease from an approved LEASE order.
     *
     * @param orderId approved order ID
     * @return created lease
     */
    Lease createLeaseFromOrder(String orderId);

    /**
     * Gets lease by ID.
     *
     * @param id lease ID
     * @return lease
     */
    Lease getLeaseById(String id);

    /**
     * Gets leases for a VLE.
     *
     * @param vleId VLE ID
     * @return list of leases
     */
    List<Lease> getLeasesByVleId(String vleId);

    /**
     * Assigns an operator to a lease.
     *
     * @param leaseId lease ID
     * @param assignment operator assignment
     * @return updated lease
     */
    Lease assignOperator(String leaseId, OperatorAssignment assignment);
}

