package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * VLE (Village Level Entrepreneur) profile entity.
 *
 * <p>Business Context:
 * - Represents business entities that lease equipment from Company
 * - VLEs serve farmers by renting equipment
 * - VLE location is administrative, not used for discovery
 *
 * <p>Uber Logic:
 * - Created by Admin
 * - Linked to User via userId
 * - Device location drives discovery, not VLE location
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "vle_profiles")
public class VleProfile extends BaseEntity {

    /**
     * Reference to User entity.
     * VLE is a user with VLE role.
     */
    @Indexed(unique = true)
    private String userId;

    /**
     * VLE type (COMPANY or INDIVIDUAL).
     */
    private String type;

    /**
     * VLE business name.
     */
    private String name;

    /**
     * VLE administrative location.
     * Not used for discovery - device location is used instead.
     */
    @GeoSpatialIndexed
    private Point location;

    /**
     * Pincode for VLE location.
     */
    private String pincode;

    /**
     * VLE status (ACTIVE, SUSPENDED, etc.).
     */
    private String status;
}

