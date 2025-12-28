package com.kissanmitra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Operator training/certification record.
 *
 * <p>Stored as nested object in Operator entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperatorTraining {

    /**
     * Device type ID for which training is valid.
     */
    private String deviceTypeId;

    /**
     * Certificate document URL (S3).
     */
    private String certificateUrl;

    /**
     * Date when training was completed.
     */
    private LocalDate completedOn;

    /**
     * Date until which certificate is valid.
     */
    private LocalDate validTill;
}

