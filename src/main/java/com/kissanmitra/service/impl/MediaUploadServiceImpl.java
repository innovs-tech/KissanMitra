package com.kissanmitra.service.impl;

import com.kissanmitra.service.MediaUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of MediaUploadService.
 *
 * <p>Business Context:
 * - Phase 1: Basic implementation with file validation
 * - Future: Integrate with AWS S3 SDK for actual uploads
 *
 * <p>Uber Logic:
 * - Validates file size and format
 * - Generates unique file names
 * - Returns URLs (placeholder for S3 URLs in Phase 1)
 */
@Slf4j
@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final List<String> ALLOWED_IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_VIDEO_FORMATS = Arrays.asList("mp4", "mov");
    private static final int MAX_TOTAL_FILES = 20;

    @Value("${aws.s3.bucket:devices-media}")
    private String s3Bucket;

    @Value("${aws.s3.region:us-east-1}")
    private String s3Region;

    @Override
    public List<String> uploadMedia(final String deviceId, final MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided");
        }

        if (files.length > MAX_TOTAL_FILES) {
            throw new IllegalArgumentException(
                    String.format("Maximum %d files allowed, got %d", MAX_TOTAL_FILES, files.length)
            );
        }

        final List<String> uploadedUrls = new ArrayList<>();

        for (final MultipartFile file : files) {
            validateFile(file);
            final String url = uploadFile(deviceId, file);
            uploadedUrls.add(url);
        }

        log.info("Uploaded {} files for device {}", uploadedUrls.size(), deviceId);
        return uploadedUrls;
    }

    @Override
    public void deleteMedia(final String deviceId, final String mediaUrl) {
        // BUSINESS DECISION: Phase 1 - Log deletion, actual S3 deletion in future phase
        log.info("Deleting media for device {}: {}", deviceId, mediaUrl);
        // TODO: Implement S3 file deletion when AWS SDK is integrated
    }

    @Override
    public void setPrimaryMedia(final String deviceId, final String mediaUrl) {
        // BUSINESS DECISION: Primary media is managed at device entity level
        // This method is for future use if needed for S3 metadata
        log.info("Setting primary media for device {}: {}", deviceId, mediaUrl);
    }

    /**
     * Validates file size and format.
     *
     * @param file file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(final MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        final String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        final String extension = getFileExtension(originalFilename).toLowerCase();
        final boolean isImage = ALLOWED_IMAGE_FORMATS.contains(extension);
        final boolean isVideo = ALLOWED_VIDEO_FORMATS.contains(extension);

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file format: %s. Allowed: %s, %s",
                            extension, String.join(", ", ALLOWED_IMAGE_FORMATS),
                            String.join(", ", ALLOWED_VIDEO_FORMATS))
            );
        }
    }

    /**
     * Uploads file to S3 and returns URL.
     *
     * <p>Phase 1: Returns placeholder URL
     * Future: Integrate with AWS S3 SDK
     *
     * @param deviceId device ID
     * @param file file to upload
     * @return S3 URL
     */
    private String uploadFile(final String deviceId, final MultipartFile file) {
        try {
            // Generate unique file name
            final String timestamp = String.valueOf(Instant.now().toEpochMilli());
            final String originalFilename = file.getOriginalFilename();
            final String extension = getFileExtension(originalFilename);
            final String uniqueFilename = String.format("%s-%s.%s",
                    timestamp, UUID.randomUUID().toString().substring(0, 8), extension);
            final String s3Key = String.format("%s/%s", deviceId, uniqueFilename);

            // BUSINESS DECISION: Phase 1 - Return placeholder URL
            // Future: Upload to S3 using AWS SDK
            final String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Bucket, s3Region, s3Key);

            log.debug("Generated S3 URL for device {}: {}", deviceId, s3Url);

            // TODO: Implement actual S3 upload when AWS SDK is integrated
            // Example:
            // s3Client.putObject(PutObjectRequest.builder()
            //     .bucket(s3Bucket)
            //     .key(s3Key)
            //     .contentType(file.getContentType())
            //     .build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Url;
        } catch (final Exception e) {
            log.error("Error uploading file for device {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename file name
     * @return file extension (without dot)
     */
    private String getFileExtension(final String filename) {
        final int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1);
    }
}

