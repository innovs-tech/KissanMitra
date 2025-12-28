package com.kissanmitra.controller;

import com.kissanmitra.enums.Response;
import com.kissanmitra.request.OtpVerifyRequest;
import com.kissanmitra.request.RoleSelectionRequest;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Controller for authentication operations.
 *
 * <p>Business Context:
 * - OTP-based authentication only
 * - Role selection after login
 * - User profile retrieval
 *
 * <p>Uber Logic:
 * - Validates input, delegates to AuthService
 * - Returns standardized responses
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Sends OTP to the given phone number.
     *
     * @param phoneNumber phone number
     * @return success message
     */
    @PostMapping("/send-otp")
    public BaseClientResponse<String> sendOtp(@RequestParam final String phoneNumber) {
        final String result = authService.sendOtp(phoneNumber);
        return Response.SUCCESS.buildSuccess(generateRequestId(), result);
    }

    /**
     * Verifies OTP and issues JWT token.
     *
     * @param otpVerifyRequest OTP verification request
     * @return AuthResponse with JWT token and user details
     */
    @PostMapping("/verify-otp")
    public BaseClientResponse<AuthResponse> verifyOtp(@Valid @RequestBody final OtpVerifyRequest otpVerifyRequest) {
        final AuthResponse result = authService.verifyOtp(
                otpVerifyRequest.getPhoneNumber(),
                otpVerifyRequest.getOtp()
        );
        return Response.SUCCESS.buildSuccess(generateRequestId(), result);
    }

    /**
     * Selects active role for current session.
     *
     * <p>Business Decision:
     * - Validates role exists in user's roles
     * - Issues new JWT token with updated active role
     *
     * @param request role selection request
     * @return new JWT token
     */
    @PostMapping("/session/role")
    public BaseClientResponse<String> selectActiveRole(@Valid @RequestBody final RoleSelectionRequest request) {
        final String token = authService.selectActiveRole(request.getActiveRole());
        return Response.SUCCESS.buildSuccess(generateRequestId(), token);
    }

    /**
     * Gets current authenticated user details.
     *
     * @return UserResponse with current user information
     */
    @GetMapping("/me")
    public BaseClientResponse<UserResponse> getCurrentUser() {
        final UserResponse user = authService.getCurrentUser();
        return Response.SUCCESS.buildSuccess(generateRequestId(), user);
    }
}

