package com.kissanmitra.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kissanmitra.dto.OperatorAssignment;
import com.kissanmitra.entity.Lease;
import com.kissanmitra.enums.Response;
import com.kissanmitra.request.CreateLeaseRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.LeaseService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for lease management.
 *
 * <p>Business Context:
 * - Only Admin can create and manage leases
 * - Leases are created from approved LEASE orders
 * - Operators are assigned to leases
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/leases")
@RequiredArgsConstructor
@org.springframework.context.annotation.ComponentScan
public class AdminLeaseController {

    private final LeaseService leaseService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Creates a lease from an approved order.
     *
     * <p>Business Context:
     * - Accepts multipart/form-data for file uploads
     * - Controller parses JSON string manually to handle Postman form-data
     * - Delegates to service after validation
     *
     * <p>Business Decision:
     * - Accepts request as String to handle Postman form-data which sends
     *   JSON parts as application/octet-stream instead of application/json
     * - Manual parsing ensures compatibility with various client tools
     *
     * @param requestJson lease creation request as JSON string (from form-data part)
     * @param attachmentFiles array of files for attachments (optional)
     * @return created lease
     * @throws JsonProcessingException if JSON parsing fails
     * @throws ConstraintViolationException if validation fails
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public BaseClientResponse<Lease> createLease(
            @RequestPart("request") final String requestJson,
            @RequestPart(value = "attachmentFiles", required = false) final MultipartFile[] attachmentFiles
    ) throws JsonProcessingException {
        // Parse JSON string to CreateLeaseRequest
        final CreateLeaseRequest request;
        try {
            request = objectMapper.readValue(requestJson, CreateLeaseRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse lease request JSON: {}", requestJson, e);
            throw new IllegalArgumentException("Invalid JSON format in request part: " + e.getMessage(), e);
        }

        // Validate request manually
        final Set<ConstraintViolation<CreateLeaseRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            violations.forEach(v -> errorMessage.append(v.getPropertyPath())
                    .append(" ").append(v.getMessage()).append("; "));
            throw new ConstraintViolationException(errorMessage.toString(), violations);
        }

        // Set files in request if provided
        if (attachmentFiles != null && attachmentFiles.length > 0) {
            request.setAttachmentFiles(Arrays.asList(attachmentFiles));
        }

        final Lease lease = leaseService.createLeaseFromOrder(request);
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

