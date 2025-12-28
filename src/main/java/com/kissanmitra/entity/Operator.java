package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import com.kissanmitra.dto.OperatorTraining;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Operator entity representing individuals who operate equipment.
 *
 * <p>Business Context:
 * - Operators are users with OPERATOR role
 * - Operators are assigned to leases (not orders or devices)
 * - Operators receive SMS notifications only (no UI access)
 * - Metrics attribution defaults to PRIMARY operator
 *
 * <p>Uber Logic:
 * - Created by Admin
 * - Linked to User via userId
 * - Training records track certifications per device type
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "operators")
public class Operator extends BaseEntity {

    /**
     * Reference to User entity.
     * Operators are users with OPERATOR role.
     */
    @Indexed(unique = true)
    private String userId;

    /**
     * Operator status (ACTIVE, SUSPENDED, ARCHIVED).
     */
    private String status;

    /**
     * Training/certification records.
     * Nested list with device type and certificate details.
     */
    private List<OperatorTraining> training;
}

