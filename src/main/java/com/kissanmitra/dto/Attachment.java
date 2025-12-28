package com.kissanmitra.dto;

import com.kissanmitra.domain.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Document attachment for a lease.
 *
 * <p>Documents are stored in S3 and referenced by URL.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Attachment {

    /**
     * Type of document.
     */
    private DocumentType type;

    /**
     * S3 URL of the document.
     */
    private String url;

    /**
     * Timestamp when document was uploaded.
     */
    private Instant uploadedAt;
}

