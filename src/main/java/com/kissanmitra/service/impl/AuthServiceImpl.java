package com.kissanmitra.service.impl;

import com.kissanmitra.entity.User;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.exception.InvalidOtpException;
import com.kissanmitra.exception.OtpExpiredException;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.security.JwtUtil;
import com.kissanmitra.service.AuthService;
import com.kissanmitra.service.UserService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.kissanmitra.config.OtpStorageService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Service implementation for authentication.
 *
 * <p>Business Context:
 * - OTP-based authentication only
 * - Mobile number is primary identifier
 * - Users created on first OTP verification
 *
 * <p>Uber Logic:
 * - Generates and sends OTP via SMS
 * - Validates OTP and creates/updates user
 * - Issues JWT token with user roles (activeRole initially null)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String OTP_KEY_PREFIX = "OTP:";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_LENGTH = 6;

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.fromNumber}")
    private String fromNumber;

    private final OtpStorageService otpStorage;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Sends OTP to the given phone number.
     *
     * <p>Business Decision:
     * - OTP valid for 5 minutes
     * - OTP stored in Redis with phone number as key
     * - Auto-formats phone number with +91 prefix if missing
     *
     * @param phoneNumber phone number
     * @return success message
     */
    @Override
    public String sendOtp(final String phoneNumber) {
        Twilio.init(accountSid, authToken);

        // BUSINESS DECISION: Generate 6-digit OTP
        final String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Save OTP for 5 minutes
        final String key = OTP_KEY_PREFIX + phoneNumber;
        otpStorage.set(key, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));

        // Format phone number for SMS
        final String rawMobile = phoneNumber.trim();
        final String mobile = rawMobile.startsWith("+91") ? rawMobile : "+91" + rawMobile;

        // Send OTP via SMS
        Message.creator(
                new com.twilio.type.PhoneNumber(mobile),
                new com.twilio.type.PhoneNumber(fromNumber),
                "Your OTP is: " + otp + " (valid for " + OTP_EXPIRY_MINUTES + " min)"
        ).create();

        log.info("OTP sent to phone: {}", phoneNumber);
        return "OTP sent successfully.";
    }

    /**
     * Verifies OTP and issues JWT token.
     *
     * <p>Business Decision:
     * - Creates user if not exists
     * - Updates lastLoginAt timestamp
     * - Issues JWT with roles (activeRole initially null)
     *
     * @param phoneNumber phone number
     * @param otp OTP code
     * @return AuthResponse with JWT token and user details
     */
    @Override
    public AuthResponse verifyOtp(final String phoneNumber, final String otp) {
        final String key = OTP_KEY_PREFIX + phoneNumber;
        final String storedOtp = otpStorage.get(key);

        if (storedOtp == null) {
            throw new OtpExpiredException("OTP expired or not found");
        }

        if (!storedOtp.equals(otp)) {
            throw new InvalidOtpException("Invalid OTP");
        }

        // Ensure user exists or get existing user
        final UserResponse userResponse = userService.ensureUserExistsOrSaveUser(phoneNumber);

        // Delete OTP after successful verification
        otpStorage.delete(key);

        // Get user entity to extract roles
        final User user = userRepository.findById(userResponse.getId())
                .orElseThrow(() -> new RuntimeException("User not found after creation"));

        // Generate JWT token with roles (activeRole initially null)
        final List<UserRole> roles = user.getRoles() != null ? user.getRoles() : List.of();
        final String token = jwtUtil.generateToken(
                user.getId(),
                phoneNumber,
                roles,
                null // activeRole set later via role selection endpoint
        );

        log.info("User authenticated: {}", phoneNumber);
        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    /**
     * Selects active role for current user session.
     *
     * <p>Business Decision:
     * - For new users with no roles, auto-assigns the selected role
     * - For existing users, validates that role exists in user's roles
     * - Updates JWT token with new active role
     * - Updates user entity to persist the role assignment
     *
     * @param activeRole role to activate
     * @return new JWT token with updated active role
     */
    @Override
    public String selectActiveRole(final UserRole activeRole) {
        // Get current user from context
        final String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        final User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // BUSINESS DECISION: For new users with no roles, auto-assign the selected role
        // This enables seamless sign-up flow where user selects their role on first login
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // Auto-assign the role for new users
            user.setRoles(List.of(activeRole));
            userRepository.save(user);
            log.info("Auto-assigned role {} to new user: {}", activeRole, currentUserId);
        } else if (!user.getRoles().contains(activeRole)) {
            // Existing user trying to select a role they don't have
            throw new RuntimeException("Role not assigned to user");
        }

        // Generate new token with updated active role
        final String token = getCurrentToken();
        return jwtUtil.updateActiveRole(token, activeRole);
    }

    /**
     * Gets current authenticated user details.
     *
     * @return UserResponse with current user information
     */
    @Override
    public UserResponse getCurrentUser() {
        final String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        return userService.getUserByUserId(currentUserId);
    }

    private String getCurrentUserId() {
        // Extract from SecurityContext via JWT token
        final org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        final String token = (String) authentication.getPrincipal();
        return jwtUtil.extractUserId(token);
    }

    private String getCurrentToken() {
        final org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }

        return (String) authentication.getPrincipal();
    }
}

