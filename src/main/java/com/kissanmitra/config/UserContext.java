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
     * <p>Business Decision:
     * - Returns null if token is invalid or cannot be parsed
     * - This allows public endpoints to work without authentication
     *
     * @return user ID, or null if not authenticated or token is invalid
     */
    public String getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        if (token == null) {
            return null;
        }

        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            // Token is invalid or cannot be parsed - treat as unauthenticated
            return null;
        }
    }

    /**
     * Gets the current authenticated user's phone number.
     *
     * <p>Business Decision:
     * - Returns null if token is invalid or cannot be parsed
     *
     * @return phone number, or null if not authenticated or token is invalid
     */
    public String getCurrentUserPhone() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        if (token == null) {
            return null;
        }

        try {
            return jwtUtil.extractPhoneNumber(token);
        } catch (Exception e) {
            // Token is invalid or cannot be parsed - treat as unauthenticated
            return null;
        }
    }

    /**
     * Gets the current authenticated user's roles.
     *
     * <p>Business Decision:
     * - Returns empty list if token is invalid or cannot be parsed
     *
     * @return list of user roles, or empty list if not authenticated or token is invalid
     */
    public List<UserRole> getCurrentUserRoles() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        final String token = extractTokenFromAuthentication(authentication);
        if (token == null) {
            return List.of();
        }

        try {
            return jwtUtil.extractRoles(token);
        } catch (Exception e) {
            // Token is invalid or cannot be parsed - treat as unauthenticated
            return List.of();
        }
    }

    /**
     * Gets the current authenticated user's active role.
     *
     * <p>Business Decision:
     * - Returns null if token is invalid or cannot be parsed
     *
     * @return active role, or null if not authenticated, not set, or token is invalid
     */
    public UserRole getCurrentUserActiveRole() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = extractTokenFromAuthentication(authentication);
        if (token == null) {
            return null;
        }

        try {
            return jwtUtil.extractActiveRole(token);
        } catch (Exception e) {
            // Token is invalid or cannot be parsed - treat as unauthenticated
            return null;
        }
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

