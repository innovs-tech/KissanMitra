package com.kissanmitra.request;

import com.kissanmitra.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for role selection.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleSelectionRequest {

    /**
     * Role to activate for current session.
     * Must be one of the user's assigned roles.
     */
    @NotNull(message = "Active role is required")
    private UserRole activeRole;
}

