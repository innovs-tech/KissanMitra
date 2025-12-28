package com.kissanmitra.service;

import com.kissanmitra.enums.UserRole;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.UserResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {
    /**
     * Sends OTP to the given phone number.
     *
     * @param phoneNumber phone number
     * @return success message
     */
    String sendOtp(String phoneNumber);

    /**
     * Verifies OTP and issues JWT token.
     *
     * @param phoneNumber phone number
     * @param otp OTP code
     * @return AuthResponse with JWT token and user details
     */
    AuthResponse verifyOtp(String phoneNumber, String otp);

    /**
     * Selects active role for current user session.
     *
     * @param activeRole role to activate
     * @return new JWT token with updated active role
     */
    String selectActiveRole(UserRole activeRole);

    /**
     * Gets current authenticated user details.
     *
     * @return UserResponse with current user information
     */
    UserResponse getCurrentUser();
}
