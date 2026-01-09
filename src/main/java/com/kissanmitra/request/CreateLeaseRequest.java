package com.kissanmitra.request;

import com.kissanmitra.domain.enums.DocumentType;
import com.kissanmitra.dto.OperatorAssignment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for creating a lease from an approved LEASE order.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateLeaseRequest {

    /**
     * Order ID for which lease is being created.
     * Must be an ACCEPTED LEASE order.
     */
    @NotNull(message = "Order ID is required")
    private String orderId;

    /**
     * Deposit amount collected for the lease.
     */
    private Double depositAmount;

    /**
     * Operators to be assigned to the lease during creation.
     * Can include PRIMARY and SECONDARY operators.
     */
    private List<OperatorAssignment> operators;

    /**
     * Document types for attachments (matches order with attachmentFiles).
     */
    private List<DocumentType> attachmentTypes;

    /**
     * Files to be uploaded for attachments.
     * Service will upload these to S3 and create Attachment objects.
     */
    private List<MultipartFile> attachmentFiles;

    /**
     * Optional notes for the lease.
     */
    private String notes;
}

