package com.kissanmitra.service;

import com.kissanmitra.config.OtpStorageService;
import com.kissanmitra.entity.User;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.exception.InvalidOtpException;
import com.kissanmitra.exception.OtpExpiredException;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.security.JwtUtil;
import com.kissanmitra.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 *
 * <p>Tests OTP sending, verification, and role selection.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private OtpStorageService otpStorage;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_OTP = "123456";
    private static final String TEST_USER_ID = "user-id";
    private static final String TEST_TOKEN = "jwt-token";

    @BeforeEach
    void setUp() {
        // Set up @Value fields using reflection
        ReflectionTestUtils.setField(authService, "accountSid", "test-sid");
        ReflectionTestUtils.setField(authService, "authToken", "test-token");
        ReflectionTestUtils.setField(authService, "fromNumber", "+1234567890");
    }

    @Test
    void testSendOtp_Success() {
        // Given
        doNothing().when(otpStorage).set(anyString(), anyString(), any());

        // When - Note: This will attempt to call Twilio API, which may fail in test environment
        // The test verifies that OTP storage is called, not the actual SMS sending
        try {
            final String result = authService.sendOtp(TEST_PHONE);
            assertEquals("OTP sent successfully.", result);
        } catch (com.twilio.exception.ApiException e) {
            // Twilio API call may fail in test environment - this is expected
            // We still verify that OTP storage was called
        }

        // Then
        verify(otpStorage, times(1)).set(anyString(), anyString(), any());
    }

    @Test
    void testSendOtp_WithCountryCode() {
        // Given
        final String phoneWithCode = "+919876543210";
        doNothing().when(otpStorage).set(anyString(), anyString(), any());

        // When - Note: This will attempt to call Twilio API, which may fail in test environment
        try {
            authService.sendOtp(phoneWithCode);
        } catch (com.twilio.exception.ApiException e) {
            // Twilio API call may fail in test environment - this is expected
        }

        // Then
        verify(otpStorage, times(1)).set(anyString(), anyString(), any());
    }

    @Test
    void testVerifyOtp_Success_NewUser() {
        // Given
        final UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        final User user = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .roles(List.of(UserRole.FARMER))
                .build();

        when(otpStorage.get(anyString())).thenReturn(TEST_OTP);
        when(userService.ensureUserExistsOrSaveUser(TEST_PHONE)).thenReturn(userResponse);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyList(), isNull()))
                .thenReturn(TEST_TOKEN);
        doNothing().when(otpStorage).delete(anyString());

        // When
        final AuthResponse result = authService.verifyOtp(TEST_PHONE, TEST_OTP);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TOKEN, result.getToken());
        assertEquals(userResponse, result.getUser());
        verify(otpStorage, times(1)).delete(anyString());
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        // Given
        when(otpStorage.get(anyString())).thenReturn("999999");

        // When & Then
        assertThrows(InvalidOtpException.class, () -> {
            authService.verifyOtp(TEST_PHONE, TEST_OTP);
        });

        verify(userService, never()).ensureUserExistsOrSaveUser(anyString());
    }

    @Test
    void testVerifyOtp_OtpExpired() {
        // Given
        when(otpStorage.get(anyString())).thenReturn(null);

        // When & Then
        assertThrows(OtpExpiredException.class, () -> {
            authService.verifyOtp(TEST_PHONE, TEST_OTP);
        });
    }

    @Test
    void testSelectActiveRole_NewUser_AutoAssign() {
        // Given
        final User user = User.builder()
                .id(TEST_USER_ID)
                .roles(null) // New user with no roles
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.updateActiveRole(anyString(), any(UserRole.class))).thenReturn(TEST_TOKEN);

        // Mock SecurityContext
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(TEST_TOKEN);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);

        // When
        final String result = authService.selectActiveRole(UserRole.FARMER);

        // Then
        assertEquals(TEST_TOKEN, result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).updateActiveRole(TEST_TOKEN, UserRole.FARMER);
    }

    @Test
    void testSelectActiveRole_ExistingUser_ValidRole() {
        // Given
        final User user = User.builder()
                .id(TEST_USER_ID)
                .roles(Arrays.asList(UserRole.FARMER, UserRole.VLE))
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(jwtUtil.updateActiveRole(anyString(), any(UserRole.class))).thenReturn(TEST_TOKEN);

        // Mock SecurityContext
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(TEST_TOKEN);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);

        // When
        final String result = authService.selectActiveRole(UserRole.VLE);

        // Then
        assertEquals(TEST_TOKEN, result);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, times(1)).updateActiveRole(TEST_TOKEN, UserRole.VLE);
    }

    @Test
    void testSelectActiveRole_InvalidRole() {
        // Given
        final User user = User.builder()
                .id(TEST_USER_ID)
                .roles(List.of(UserRole.FARMER))
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // Mock SecurityContext
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(TEST_TOKEN);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.selectActiveRole(UserRole.ADMIN); // User doesn't have ADMIN role
        });
    }

    @Test
    void testSelectActiveRole_UserNotAuthenticated() {
        // Given
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.selectActiveRole(UserRole.FARMER);
        });
    }

    @Test
    void testGetCurrentUser_Success() {
        // Given
        final UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userService.getUserByUserId(TEST_USER_ID)).thenReturn(userResponse);

        // Mock SecurityContext
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(TEST_TOKEN);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);

        // When
        final UserResponse result = authService.getCurrentUser();

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
    }

    @Test
    void testGetCurrentUser_NotAuthenticated() {
        // Given
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.getCurrentUser();
        });
    }
}

