package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.domain.enums.LeaseStatus;
import com.kissanmitra.dto.Attachment;
import com.kissanmitra.dto.Commitment;
import com.kissanmitra.dto.OperatorAssignment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * Lease entity representing long-term equipment control from Company to VLE.
 *
 * <p>Business Context:
 * - Created from approved LEASE orders
 * - Represents actual equipment control (not just intent)
 * - Operators are assigned to leases
 * - Telemetry is attributed to lease operators
 *
 * <p>Uber Logic:
 * - Created when LEASE order is approved
 * - Device.currentLeaseId is set when lease is active
 * - Operators assigned during lease lifecycle
 * - Billing finalized when lease is completed
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "leases")
public class Lease extends BaseEntity {

    /**
     * Reference to device being leased.
     */
    @Indexed
    private String deviceId;

    /**
     * Reference to VLE receiving the lease.
     */
    @Indexed
    private String vleId;

    /**
     * Lease status.
     */
    private LeaseStatus status;

    /**
     * Commitment details (hours or acres).
     */
    private Commitment commitment;

    /**
     * Estimated price for the lease.
     */
    private Double estimatedPrice;

    /**
     * Deposit amount collected.
     */
    private Double depositAmount;

    /**
     * Lease start date.
     */
    private LocalDate startDate;

    /**
     * Lease end date.
     * Null if lease is ongoing.
     */
    private LocalDate endDate;

    /**
     * Operators assigned to this lease.
     * Embedded list with role (PRIMARY/SECONDARY).
     */
    private List<OperatorAssignment> operators;

    /**
     * Admin ID who signed/approved the lease.
     */
    private String signedByAdminId;

    /**
     * Document attachments (lease agreements, etc.).
     */
    private List<Attachment> attachments;

    /**
     * Optional notes for the lease.
     */
    private String notes;
}

