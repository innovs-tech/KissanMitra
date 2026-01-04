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
 * Request DTO for updating a manufacturer.
 *
 * <p>Business Context:
 * - Code cannot be changed (immutable)
 * - Only name and active can be updated
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateManufacturerRequest {

    /**
     * Manufacturer display name (e.g., "Mahindra Tractors", "John Deere India").
     * Can be updated.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * Whether this manufacturer is active.
     */
    @NotNull(message = "active is required")
    private Boolean active;
}

