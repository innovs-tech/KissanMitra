package com.kissanmitra.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for uploading media files to S3.
 *
 * <p>Business Context:
 * - Media files (images/videos) are uploaded to S3
 * - URLs are stored in device.mediaUrls
 * - One media must be marked as primary (thumbnail)
 *
 * <p>Uber Logic:
 * - Validates file size (max 20MB) and format (JPG, PNG, MP4, MOV)
 * - Uploads files to S3 with unique names
 * - Returns public URLs for storage in device entity
 */
public interface MediaUploadService {

    /**
     * Uploads media files to S3.
     *
     * <p>Business Decision:
     * - Max 20 files total (photos + videos combined)
     * - Per-file size limit: 20MB
     * - Supported formats: JPG, PNG, MP4, MOV
     * - Files stored with unique names: {deviceId}/{timestamp}-{originalFilename}
     *
     * @param deviceId device ID for organizing files
     * @param files array of files to upload
     * @return list of S3 URLs
     * @throws IllegalArgumentException if file validation fails
     */
    List<String> uploadMedia(String deviceId, MultipartFile[] files);

    /**
     * Deletes a media file from S3.
     *
     * @param deviceId device ID
     * @param mediaUrl S3 URL of file to delete
     */
    void deleteMedia(String deviceId, String mediaUrl);

    /**
     * Sets primary media URL (thumbnail).
     *
     * <p>Business Decision:
     * - One media must be marked as primary
     * - Primary media shown in listing/thumbnail
     *
     * @param deviceId device ID
     * @param mediaUrl S3 URL to set as primary
     */
    void setPrimaryMedia(String deviceId, String mediaUrl);
}

