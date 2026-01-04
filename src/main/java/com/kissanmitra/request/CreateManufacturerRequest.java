package com.kissanmitra.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a manufacturer.
 *
 * <p>Business Context:
 * - Code must be unique and uppercase (e.g., "MAHINDRA", "JOHN_DEERE")
 * - Code is immutable after creation
 * - Name can be updated later
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateManufacturerRequest {

    /**
     * Manufacturer code (e.g., MAHINDRA, JOHN_DEERE).
     * Must be uppercase, alphanumeric with underscores.
     * Immutable after creation.
     */
    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric with underscores")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    /**
     * Manufacturer display name (e.g., "Mahindra Tractors", "John Deere India").
     * Can be updated later.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * Whether this manufacturer is active.
     * Defaults to true if not provided.
     */
    private Boolean active;
}

