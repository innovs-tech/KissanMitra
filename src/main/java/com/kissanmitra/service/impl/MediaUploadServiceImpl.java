package com.kissanmitra.service.impl;

import com.kissanmitra.service.MediaUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of MediaUploadService.
 *
 * <p>Business Context:
 * - Uploads media files (images/videos) to AWS S3
 * - Validates file size and format
 * - Generates unique file names
 *
 * <p>Uber Logic:
 * - Validates file size and format
 * - Generates unique file names
 * - Uploads files to S3 and returns URLs
 */
@Slf4j
@Service
public class MediaUploadServiceImpl extends BaseS3UploadService implements MediaUploadService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final List<String> ALLOWED_IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_VIDEO_FORMATS = Arrays.asList("mp4", "mov");
    private static final int MAX_TOTAL_FILES = 20;

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
     * <p>Business Decision:
     * - Uploads files to S3 using AWS SDK
     * - Returns S3 URL for uploaded file
     *
     * @param deviceId device ID
     * @param file file to upload
     * @return S3 URL
     */
    private String uploadFile(final String deviceId, final MultipartFile file) {
        final String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        final String uniqueFilename = generateUniqueFilename(originalFilename);
        // BUSINESS DECISION: Store device media under devices/ prefix
        final String s3Key = String.format("devices/%s/%s", deviceId, uniqueFilename);

        return uploadFileToS3(s3Key, file);
    }
}

