package com.kissanmitra.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for uploading document files to S3.
 *
 * <p>Business Context:
 * - Document files (PDFs, images) are uploaded to S3
 * - URLs are stored in lease.attachments or operator profiles
 *
 * <p>Uber Logic:
 * - Validates file size (max 10MB) and format (PDF, JPG, PNG)
 * - Uploads files to S3 with unique names
 * - Returns public URLs for storage in entities
 */
public interface DocumentUploadService {

    /**
     * Uploads document files to S3.
     *
     * <p>Business Decision:
     * - Max 10 files per upload
     * - Per-file size limit: 10MB
     * - Supported formats: PDF, JPG, PNG
     * - Files stored with unique names: {entityType}/{entityId}/{timestamp}-{originalFilename}
     *
     * @param entityType entity type (e.g., "leases", "operators")
     * @param entityId entity ID for organizing files
     * @param files array of files to upload
     * @return list of S3 URLs
     * @throws IllegalArgumentException if file validation fails
     */
    List<String> uploadDocuments(String entityType, String entityId, MultipartFile[] files);
}

