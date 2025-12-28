package com.kissanmitra.config;

import com.kissanmitra.enums.UserRole;
import com.kissanmitra.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Helper class for extracting user context from SecurityContext.
 *
 * <p>Provides convenient methods to access authenticated user information
 * from Spring Security context in service layer.
 */
@Component
@RequiredArgsConstructor
public class UserContext {

    private final JwtUtil jwtUtil;

    /**
     * Gets the current authenticated user's ID.
     *
     * @return user ID, or null if not authenticated
     */
    public String getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        return token != null ? jwtUtil.extractUserId(token) : null;
    }

    /**
     * Gets the current authenticated user's phone number.
     *
     * @return phone number, or null if not authenticated
     */
    public String getCurrentUserPhone() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        return token != null ? jwtUtil.extractPhoneNumber(token) : null;
    }

    /**
     * Gets the current authenticated user's roles.
     *
     * @return list of user roles, or empty list if not authenticated
     */
    public List<UserRole> getCurrentUserRoles() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        final String token = extractTokenFromAuthentication(authentication);
        return token != null ? jwtUtil.extractRoles(token) : List.of();
    }

    /**
     * Gets the current authenticated user's active role.
     *
     * @return active role, or null if not authenticated or not set
     */
    public UserRole getCurrentUserActiveRole() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        return token != null ? jwtUtil.extractActiveRole(token) : null;
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(final UserRole role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Checks if the current user's active role matches the specified role.
     *
     * @param role role to check
     * @return true if active role matches, false otherwise
     */
    public boolean isActiveRole(final UserRole role) {
        final UserRole activeRole = getCurrentUserActiveRole();
        return activeRole != null && activeRole.equals(role);
    }

    private String extractTokenFromAuthentication(final Authentication authentication) {
        // Token is stored as principal in authentication object
        if (authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
}

