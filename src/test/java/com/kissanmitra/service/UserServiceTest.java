package com.kissanmitra.service;

import com.kissanmitra.dto.UserProfile;
import com.kissanmitra.entity.User;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.enums.UserStatus;
import com.kissanmitra.mapper.UserMapper;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.request.UserRequest;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * <p>Tests user creation, updates, and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String TEST_PHONE = "+919876543210";
    private static final String TEST_USER_ID = "user-id-123";

    @Test
    void testEnsureUserExistsOrSaveUser_NewUser() {
        // Given
        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(null);
        
        User savedUser = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .roles(Collections.singletonList(UserRole.FARMER))
                .activeRole(UserRole.FARMER)
                .status(UserStatus.ACTIVE)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .roles(Collections.singletonList(UserRole.FARMER))
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.mapToResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.ensureUserExistsOrSaveUser(TEST_PHONE);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testEnsureUserExistsOrSaveUser_ExistingUser() {
        // Given
        User existingUser = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(existingUser);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.mapToResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.ensureUserExistsOrSaveUser(TEST_PHONE);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class)); // Updates lastLoginAt
    }

    @Test
    void testUpdateUserInfo_Success() {
        // Given
        User existingUser = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .profile(UserProfile.builder()
                        .name("Old Name")
                        .build())
                .build();

        UserRequest updateRequest = UserRequest.builder()
                .phoneNumber(TEST_PHONE)
                .name("New Name")
                .build();

        User updatedUser = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .profile(UserProfile.builder()
                        .name("New Name")
                        .build())
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .profile(UserProfile.builder()
                        .name("New Name")
                        .build())
                .build();

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(existingUser);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.mapToResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.updateUserInfo(updateRequest);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUserInfo_UserNotFound() {
        // Given
        UserRequest updateRequest = UserRequest.builder()
                .phoneNumber(TEST_PHONE)
                .build();

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.updateUserInfo(updateRequest);
        });
    }

    @Test
    void testGetUserByUserId_Success() {
        // Given
        User user = User.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(TEST_USER_ID)
                .phone(TEST_PHONE)
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(java.util.Optional.of(user));
        when(userMapper.mapToResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.getUserByUserId(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
    }

    @Test
    void testGetUserByUserId_NotFound() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.getUserByUserId(TEST_USER_ID);
        });
    }
}

