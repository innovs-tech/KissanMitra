package com.kissanmitra.domain.enums;

/**
 * Represents the type of document attached to a lease or operator profile.
 *
 * <p>Documents are stored in S3 and referenced by URL in entities.
 */
public enum DocumentType {
    /**
     * Official lease agreement document.
     */
    LEASE_AGREEMENT,

    /**
     * Training certificate for operator.
     */
    TRAINING_CERT,

    /**
     * Identity proof document.
     */
    IDENTITY_PROOF,

    /**
     * Other miscellaneous documents.
     */
    OTHER
}

