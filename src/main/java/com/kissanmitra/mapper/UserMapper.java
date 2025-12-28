package com.kissanmitra.mapper;

import com.kissanmitra.entity.User;
import com.kissanmitra.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entity to UserResponse DTO.
 *
 * <p>Manual implementation is used because MapStruct has issues
 * detecting Lombok-generated getters when using @SuperBuilder with inheritance.
 */
@Component
public class UserMapper {

    /**
     * Maps User entity to UserResponse DTO.
     *
     * <p>Maps all fields including inherited ones from BaseEntity (id, createdAt, updatedAt).
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    public UserResponse mapToResponse(final User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .roles(user.getRoles())
                .activeRole(user.getActiveRole())
                .profile(user.getProfile())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
