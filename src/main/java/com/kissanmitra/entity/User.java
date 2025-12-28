package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.dto.UserProfile;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * User entity representing a real person authenticated via OTP.
 *
 * <p>Business Context:
 * - Users can have multiple roles (FARMER, VLE, ADMIN, OPERATOR)
 * - Exactly one active role per session
 * - Mobile number is the primary identifier
 *
 * <p>Uber Logic:
 * - User is created on first OTP verification
 * - Roles are assigned by Admin or during profile setup
 * - Active role determines access permissions and UI flow
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "users")
public class User extends BaseEntity {

    /**
     * Mobile phone number.
     * Primary identifier for authentication.
     * Unique across all users.
     */
    @Indexed(unique = true)
    private String phone;

    /**
     * List of roles assigned to this user.
     * A user can have multiple roles (e.g., FARMER and VLE).
     */
    private List<UserRole> roles;

    /**
     * Currently active role for this session.
     * Null initially, set after role selection.
     * Determines access permissions and UI flow.
     */
    private UserRole activeRole;

    /**
     * User profile information.
     * Contains name, default location, and pincode.
     */
    private UserProfile profile;

    /**
     * User account status.
     * ACTIVE: User can log in and use the system
     * INACTIVE: User account is suspended
     */
    private UserStatus status;

    /**
     * Timestamp of last successful login.
     * Updated on each OTP verification.
     */
    private Instant lastLoginAt;
}

