package com.kissanmitra.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a device type.
 *
 * <p>Business Context:
 * - Code must be unique and uppercase (e.g., "TRACTOR", "HARVESTER")
 * - Code is immutable after creation
 * - DisplayName can be updated later
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateDeviceTypeRequest {

    /**
     * Device type code (e.g., TRACTOR, HARVESTER).
     * Must be uppercase, alphanumeric with underscores.
     * Immutable after creation.
     */
    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric with underscores")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    /**
     * Display name for the device type (e.g., "Heavy Duty Tractor").
     * Can be updated later.
     */
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    /**
     * Whether this device type requires an operator.
     */
    @NotNull(message = "requiresOperator is required")
    private Boolean requiresOperator;

    /**
     * Whether this device type is active.
     * Defaults to true if not provided.
     */
    private Boolean active;
}

