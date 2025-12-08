package com.kissanmitra.entity;

import com.kissanmitra.dto.Address;
import com.kissanmitra.enums.UserRole;
import com.kissanmitra.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Setter
@Getter
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed( unique = true)
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
