package com.kissanmitra.service.impl;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.domain.enums.DocumentType;
import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import com.kissanmitra.domain.enums.PricingMetric;
import com.kissanmitra.dto.Attachment;
import com.kissanmitra.dto.Commitment;
import com.kissanmitra.dto.OperatorAssignment;
import com.kissanmitra.dto.PricingRuleItem;
import com.kissanmitra.entity.Device;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.entity.Order;
import com.kissanmitra.entity.PricingRule;
import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.DeviceRepository;
import com.kissanmitra.repository.LeaseRepository;
import com.kissanmitra.repository.OrderRepository;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.request.CreateLeaseRequest;
import com.kissanmitra.service.DocumentUploadService;
import com.kissanmitra.service.LeaseService;
import com.kissanmitra.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final VleProfileRepository vleProfileRepository;
    private final PricingService pricingService;
    private final DocumentUploadService documentUploadService;
    private final UserContext userContext;
    private final com.kissanmitra.service.SmsNotificationService smsNotificationService;

    /**
     * Creates a lease from an approved LEASE order.
     *
     * <p>Business Decision:
     * - Only APPROVED LEASE orders can create leases
     * - Device.currentLeaseId is set
     * - Lease status is ACTIVE
     * - Operators, attachments, and deposit amount can be set during creation
     *
     * @param request lease creation request containing orderId and additional details
     * @return created lease
     */
    @Override
    @Transactional
    public Lease createLeaseFromOrder(final CreateLeaseRequest request) {
        final Order order = orderRepository.findById(request.getOrderId())
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

        // BUSINESS DECISION: For LEASE orders, the requester is the VLE (user) who wants to lease the device
        // Get VLE profile ID from user ID
        final String requesterUserId = order.getRequestedBy();
        if (requesterUserId == null) {
            throw new RuntimeException("Requester (VLE user ID) not found in order");
        }

        final VleProfile vleProfile = vleProfileRepository.findByUserId(requesterUserId)
                .orElseThrow(() -> new RuntimeException("VLE profile not found for requester"));

        final String vleId = vleProfile.getId();

        // Create commitment from order
        final Commitment commitment = Commitment.builder()
                .type(com.kissanmitra.domain.enums.CommitmentType.HOURS) // Default to HOURS
                .value(order.getRequestedHours() != null ? order.getRequestedHours() : 0.0)
                .build();

        // Calculate estimated price
        final Double estimatedPrice = calculateEstimatedPrice(order);

        // Upload files and build attachments
        final List<Attachment> attachments = buildAttachmentsFromFiles(
                request.getOrderId(),
                request.getAttachmentTypes(),
                request.getAttachmentFiles()
        );

        // Create lease
        final Lease lease = buildLeaseFromOrder(order, vleId, commitment, estimatedPrice, request, attachments);

        final Lease saved = leaseRepository.save(lease);

        // Update device with current lease ID
        device.setCurrentLeaseId(saved.getId());
        deviceRepository.save(device);

        log.info("Created lease: {} from order: {} for device: {}", saved.getId(), request.getOrderId(), order.getDeviceId());
        
        // Send notification
        smsNotificationService.notifyLeaseCreated(saved);
        
        return saved;
    }

    /**
     * Calculates estimated price from pricing rules based on order details.
     *
     * <p>Business Decision:
     * - Uses order startDate to get active pricing rule (time-specific or default)
     * - Calculation uses only ONE metric: PER_HOUR (if requestedHours provided) OR PER_ACRE (if requestedAcres provided)
     *
     * @param order order containing pricing details
     * @return estimated price or null if calculation fails
     */
    private Double calculateEstimatedPrice(final Order order) {
        try {
            final PricingRule pricingRule = pricingService.getActivePricingForDevice(
                    order.getDeviceId(),
                    order.getStartDate() != null ? order.getStartDate() : LocalDate.now()
            );

            if (pricingRule != null && pricingRule.getRules() != null && !pricingRule.getRules().isEmpty()) {
                // BUSINESS DECISION: Use only one metric - prefer PER_HOUR if requestedHours is provided, else PER_ACRE
                if (order.getRequestedHours() != null) {
                    // Calculate based on PER_HOUR metric
                    for (final PricingRuleItem ruleItem : pricingRule.getRules()) {
                        if (ruleItem.getMetric() == PricingMetric.PER_HOUR) {
                            return ruleItem.getRate() * order.getRequestedHours();
                        }
                    }
                } else if (order.getRequestedAcres() != null) {
                    // Calculate based on PER_ACRE metric
                    for (final PricingRuleItem ruleItem : pricingRule.getRules()) {
                        if (ruleItem.getMetric() == PricingMetric.PER_ACRE) {
                            return ruleItem.getRate() * order.getRequestedAcres();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not calculate estimated price for lease: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Uploads files to S3 and builds Attachment objects.
     *
     * <p>Business Decision:
     * - Files are uploaded to S3 using DocumentUploadService
     * - Attachment types are matched with files by index
     * - uploadedAt timestamp is set to current time
     *
     * @param orderId order ID for organizing files in S3
     * @param attachmentTypes list of document types (matches files by index)
     * @param attachmentFiles list of files to upload
     * @return list of Attachment objects with S3 URLs
     */
    private List<Attachment> buildAttachmentsFromFiles(
            final String orderId,
            final List<DocumentType> attachmentTypes,
            final List<MultipartFile> attachmentFiles
    ) {
        // If no files provided, return empty list
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            return List.of();
        }

        // Upload files to S3
        final MultipartFile[] filesArray = attachmentFiles.toArray(new MultipartFile[0]);
        final List<String> uploadedUrls = documentUploadService.uploadDocuments("leases", orderId, filesArray);

        // Build Attachment objects with types and uploaded URLs
        final List<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < uploadedUrls.size(); i++) {
            // Match attachment type by index (first type -> first file, etc.)
            final DocumentType type = (attachmentTypes != null && i < attachmentTypes.size())
                    ? attachmentTypes.get(i)
                    : DocumentType.OTHER; // Default to OTHER if type not provided

            attachments.add(Attachment.builder()
                    .type(type)
                    .url(uploadedUrls.get(i)) // S3 URL from upload
                    .uploadedAt(Instant.now())
                    .build());
        }

        return attachments;
    }

    /**
     * Builds a Lease entity from order details and request.
     *
     * @param order order containing lease details
     * @param vleId VLE profile ID
     * @param commitment commitment details
     * @param estimatedPrice calculated estimated price
     * @param request lease creation request with additional details
     * @param attachments uploaded attachments with S3 URLs
     * @return built Lease entity
     */
    private Lease buildLeaseFromOrder(
            final Order order,
            final String vleId,
            final Commitment commitment,
            final Double estimatedPrice,
            final CreateLeaseRequest request,
            final List<Attachment> attachments
    ) {
        // BUSINESS DECISION: Set operators with assignedAt timestamp if provided
        // Only one PRIMARY operator allowed, multiple SECONDARY operators allowed
        final List<com.kissanmitra.dto.OperatorAssignment> operators = new ArrayList<>();
        if (request.getOperators() != null && !request.getOperators().isEmpty()) {
            boolean hasPrimary = false;
            for (final com.kissanmitra.dto.OperatorAssignment assignment : request.getOperators()) {
                if (assignment.getRole() == com.kissanmitra.domain.enums.OperatorRole.PRIMARY) {
                    if (hasPrimary) {
                        log.warn("Multiple PRIMARY operators provided, using first one only");
                        continue; // Skip additional PRIMARY operators
                    }
                    hasPrimary = true;
                }
                assignment.setAssignedAt(Instant.now());
                operators.add(assignment);
            }
        }

        return Lease.builder()
                .deviceId(order.getDeviceId())
                .vleId(vleId)
                .status(LeaseStatus.ACTIVE)
                .commitment(commitment)
                .estimatedPrice(estimatedPrice)
                .depositAmount(request.getDepositAmount())
                .startDate(order.getStartDate())
                .endDate(order.getEndDate())
                .operators(operators)
                .attachments(attachments)
                .signedByAdminId(userContext.getCurrentUserId())
                .notes(request.getNotes() != null ? request.getNotes() : order.getNote())
                .build();
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

        // Send notification
        smsNotificationService.notifyOperatorAssigned(saved, assignment.getOperatorId());

        return saved;
    }
}

