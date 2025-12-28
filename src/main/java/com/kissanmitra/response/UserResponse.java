package com.kissanmitra.response;

import com.kissanmitra.dto.UserProfile;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for User entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String phone;
    private List<UserRole> roles;
    private UserRole activeRole;
    private UserProfile profile;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}
