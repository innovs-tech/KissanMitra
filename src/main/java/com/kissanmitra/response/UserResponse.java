package com.kissanmitra.response;

import com.kissanmitra.dto.Address;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String phoneNumber;
    private String name;
    private Address address;
    private String pinCode;
    private String companyId;
    private UserRole role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
