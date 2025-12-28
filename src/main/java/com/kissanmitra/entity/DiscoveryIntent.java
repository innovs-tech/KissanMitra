package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Discovery intent entity for pre-login interest capture.
 *
 * <p>Business Context:
 * - Created when user shows interest before login
 * - Temporary record with TTL (10-30 minutes)
 * - Converted to Order after login
 *
 * <p>Uber Logic:
 * - Allows discovery without authentication
 * - Intent captured and converted to order after login
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "discovery_intents")
public class DiscoveryIntent extends BaseEntity {

    /**
     * Reference to device.
     */
    @Indexed
    private String deviceId;

    /**
     * Intent type (RENT or LEASE).
     */
    private String intentType;

    /**
     * Requested hours.
     */
    private Double requestedHours;

    /**
     * Requested acres.
     */
    private Double requestedAcres;

    /**
     * Intent status (CREATED, CONSUMED, EXPIRED, REJECTED).
     */
    private String status;

    /**
     * Expiration timestamp.
     * TTL index will auto-delete expired intents.
     */
    @Indexed
    private Instant expiresAt;
}

