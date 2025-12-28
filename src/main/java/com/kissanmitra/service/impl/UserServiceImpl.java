package com.kissanmitra.service.impl;

import com.kissanmitra.entity.User;
import com.kissanmitra.enums.UserStatus;
import com.kissanmitra.mapper.UserMapper;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.request.UserRequest;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service implementation for user management.
 *
 * <p>Business Context:
 * - Users are created on first OTP verification
 * - User profile can be updated after creation
 * - Roles are assigned by Admin or during profile setup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Ensures a user exists for the given phone number, creating one if necessary.
     *
     * <p>Business Decision:
     * - Creates user with ACTIVE status on first OTP verification
     * - Sets lastLoginAt timestamp
     *
     * @param phoneNumber phone number
     * @return UserResponse
     */
    @Override
    public UserResponse ensureUserExistsOrSaveUser(final String phoneNumber) {
        User user = userRepository.findByPhone(phoneNumber);

        if (user == null) {
            // BUSINESS DECISION: Create user with ACTIVE status on first login
            user = User.builder()
                    .phone(phoneNumber)
                    .status(UserStatus.ACTIVE)
                    .roles(List.of()) // Empty roles initially, assigned later
                    .build();
            user = userRepository.save(user);
            log.info("Created new user with phone: {}", phoneNumber);
        } else {
            // Update last login timestamp
            user.setLastLoginAt(Instant.now());
            user = userRepository.save(user);
        }

        return userMapper.mapToResponse(user);
    }

    /**
     * Updates user information.
     *
     * <p>Business Decision:
     * - Supports partial updates (only provided fields are updated)
     * - Android app can send either defaultLocation or pincode
     * - Can be called in background to update location as needed
     *
     * @param userRequest user update request
     * @return updated UserResponse
     */
    @Override
    public UserResponse updateUserInfo(final UserRequest userRequest) {
        final User existingUser = userRepository.findByPhone(userRequest.getPhoneNumber());

        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        // Update only provided fields
        final User.UserBuilder<?, ?> builder = existingUser.toBuilder();

        // Build or update profile
        com.kissanmitra.dto.UserProfile.UserProfileBuilder profileBuilder;
        if (existingUser.getProfile() != null) {
            profileBuilder = existingUser.getProfile().toBuilder();
        } else {
            profileBuilder = com.kissanmitra.dto.UserProfile.builder();
        }

        // Update name if provided
        if (userRequest.getName() != null && !userRequest.getName().trim().isEmpty()) {
            profileBuilder.name(userRequest.getName().trim());
            log.debug("Updating name for user: {}", userRequest.getPhoneNumber());
        }

        // Update defaultLocation if provided (Android app sends this when permission granted)
        if (userRequest.getDefaultLocation() != null) {
            profileBuilder.defaultLocation(userRequest.getDefaultLocation());
            log.debug("Updating defaultLocation for user: {}", userRequest.getPhoneNumber());
        }

        // Update pincode if provided (Android app sends this when permission denied)
        if (userRequest.getPincode() != null && !userRequest.getPincode().trim().isEmpty()) {
            profileBuilder.pincode(userRequest.getPincode().trim());
            log.debug("Updating pincode for user: {}", userRequest.getPhoneNumber());
        }

        builder.profile(profileBuilder.build());

        final User updatedUser = builder.build();
        final User savedUser = userRepository.save(updatedUser);

        log.info("Updated user profile for user: {}", userRequest.getPhoneNumber());
        return userMapper.mapToResponse(savedUser);
    }

    /**
     * Gets user by user ID.
     *
     * @param id user ID
     * @return UserResponse
     */
    @Override
    public UserResponse getUserByUserId(final String id) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.mapToResponse(user);
    }
}
