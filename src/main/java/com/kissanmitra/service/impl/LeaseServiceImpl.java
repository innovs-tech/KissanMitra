package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.dto.Commitment;
import com.kissanmitra.dto.OperatorAssignment;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.LeaseRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.service.LeaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for lease management.
 *
 * <p>Business Context:
 * - Leases are created from approved LEASE orders
 * - Leases represent actual equipment control (not just intent)
 * - Operators are assigned to leases
 * - Device.currentLeaseId is set when lease is active
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseServiceImpl implements LeaseService {

    private final LeaseRepository leaseRepository;
    private final OrderRepository orderRepository;
    private final DeviceRepository deviceRepository;
    private final UserContext userContext;

    /**
     * Creates a lease from an approved LEASE order.
     *
     * <p>Business Decision:
     * - Only APPROVED LEASE orders can create leases
     * - Device.currentLeaseId is set
     * - Lease status is ACTIVE
     *
     * @param orderId approved order ID
     * @return created lease
     */
    @Override
    @Transactional
    public Lease createLeaseFromOrder(final String orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // BUSINESS DECISION: Only ACCEPTED LEASE orders can create leases
        if (order.getOrderType() != OrderType.LEASE) {
            throw new RuntimeException("Only LEASE orders can create leases");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Order must be ACCEPTED to create lease");
        }

        final Device device = deviceRepository.findById(order.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // BUSINESS DECISION: Device must be available
        if (device.getCurrentLeaseId() != null) {
            throw new RuntimeException("Device is already leased");
        }

        // Extract VLE ID from order (assuming order.handledBy.id is VLE ID for LEASE orders)
        // TODO: This logic needs refinement based on actual business rules
        final String vleId = order.getHandledBy() != null ? order.getHandledBy().getId() : null;
        if (vleId == null) {
            throw new RuntimeException("VLE ID not found in order");
        }

        // Create commitment from order
        final Commitment commitment = Commitment.builder()
                .type(com.kissanmitra.domain.enums.CommitmentType.HOURS) // Default to HOURS
                .value(order.getRequestedHours() != null ? order.getRequestedHours() : 0.0)
                .build();

        // Create lease
        final Lease lease = Lease.builder()
                .deviceId(order.getDeviceId())
                .vleId(vleId)
                .status(LeaseStatus.ACTIVE)
                .commitment(commitment)
                .startDate(LocalDate.now())
                .operators(new ArrayList<>())
                .signedByAdminId(userContext.getCurrentUserId())
                .notes(order.getNote())
                .build();

        final Lease saved = leaseRepository.save(lease);

        // Update device with current lease ID
        device.setCurrentLeaseId(saved.getId());
        deviceRepository.save(device);

        log.info("Created lease: {} from order: {} for device: {}", saved.getId(), orderId, order.getDeviceId());
        return saved;
    }

    /**
     * Gets lease by ID.
     *
     * @param id lease ID
     * @return lease
     */
    @Override
    public Lease getLeaseById(final String id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lease not found"));
    }

    /**
     * Gets leases for a VLE.
     *
     * @param vleId VLE ID
     * @return list of leases
     */
    @Override
    public List<Lease> getLeasesByVleId(final String vleId) {
        return leaseRepository.findByVleIdAndStatus(vleId, LeaseStatus.ACTIVE);
    }

    /**
     * Assigns an operator to a lease.
     *
     * <p>Business Decision:
     * - Only one PRIMARY operator allowed
     * - Multiple SECONDARY operators allowed
     *
     * @param leaseId lease ID
     * @param assignment operator assignment
     * @return updated lease
     */
    @Override
    @Transactional
    public Lease assignOperator(final String leaseId, final OperatorAssignment assignment) {
        final Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        // BUSINESS DECISION: Only one PRIMARY operator allowed
        if (assignment.getRole() == com.kissanmitra.domain.enums.OperatorRole.PRIMARY) {
            final List<OperatorAssignment> operators = lease.getOperators() != null
                    ? new ArrayList<>(lease.getOperators())
                    : new ArrayList<>();

            // Remove existing PRIMARY operator
            operators.removeIf(op -> op.getRole() == com.kissanmitra.domain.enums.OperatorRole.PRIMARY);

            // Add new PRIMARY operator
            assignment.setAssignedAt(Instant.now());
            operators.add(assignment);
            lease.setOperators(operators);
        } else {
            // Add SECONDARY operator
            final List<OperatorAssignment> operators = lease.getOperators() != null
                    ? new ArrayList<>(lease.getOperators())
                    : new ArrayList<>();
            assignment.setAssignedAt(Instant.now());
            operators.add(assignment);
            lease.setOperators(operators);
        }

        final Lease saved = leaseRepository.save(lease);
        log.info("Assigned operator: {} to lease: {}", assignment.getOperatorId(), leaseId);

        // TODO: Send SMS notification to operator

        return saved;
    }
}

