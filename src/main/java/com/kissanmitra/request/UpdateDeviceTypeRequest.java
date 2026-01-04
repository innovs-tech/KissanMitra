package com.kissanmitra.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating a device type.
 *
 * <p>Business Context:
 * - Code cannot be changed (immutable)
 * - Only displayName, requiresOperator, and active can be updated
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateDeviceTypeRequest {

    /**
     * Display name for the device type (e.g., "Heavy Duty Tractor").
     * Can be updated.
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
     */
    @NotNull(message = "active is required")
    private Boolean active;
}

