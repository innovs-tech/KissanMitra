package com.kissanmitra.request;

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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserRequest {
    private String phoneNumber;
    private String name;
    private Address address;
    private String pinCode;
    private String companyId;
    private UserRole role;
}
