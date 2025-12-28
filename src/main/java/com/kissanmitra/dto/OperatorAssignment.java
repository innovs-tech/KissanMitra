package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.OperatorRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Operator assignment to a lease.
 *
 * <p>Operators are assigned to leases (not orders or devices).
 * Metrics attribution defaults to PRIMARY operator.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperatorAssignment {

    /**
     * Operator ID.
     */
    private String operatorId;

    /**
     * Operator role (PRIMARY or SECONDARY).
     */
    private OperatorRole role;

    /**
     * Timestamp when operator was assigned.
     */
    private Instant assignedAt;
}

