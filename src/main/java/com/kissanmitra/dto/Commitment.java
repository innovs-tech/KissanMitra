package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.CommitmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Commitment details for a lease.
 *
 * <p>Specifies the commitment metric (hours or acres) and value.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Commitment {

    /**
     * Type of commitment (HOURS or ACRES).
     */
    private CommitmentType type;

    /**
     * Commitment value.
     */
    private Double value;
}

