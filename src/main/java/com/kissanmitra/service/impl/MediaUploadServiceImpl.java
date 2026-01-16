package com.kissanmitra.service.impl;

import com.kissanmitra.service.MediaUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        log.info("Deleting media for device {}: {}", deviceId, mediaUrl);
        
        // Extract S3 key from URL
        final String s3Key = extractS3KeyFromUrl(mediaUrl);
        if (s3Key == null) {
            log.warn("Could not extract S3 key from media URL: {}, skipping S3 deletion", mediaUrl);
            return;
        }
        
        // Delete from S3
        deleteFileFromS3(s3Key);
        log.info("Successfully deleted media from S3 for device {}: {}", deviceId, s3Key);
    }

    @Override
    public void setPrimaryMedia(final String deviceId, final String mediaUrl) {
        // BUSINESS DECISION: Primary media is managed at device entity level (DeviceController)
        // This method exists for interface compliance but primary media is set via Device entity
        // Future enhancement: Could add S3 metadata tagging if needed
        log.info("Primary media set for device {}: {} (managed at entity level)", deviceId, mediaUrl);
    }

    @Override
    public List<String> refreshMediaUrls(final List<String> storedUrls) {
        if (storedUrls == null || storedUrls.isEmpty()) {
            return new ArrayList<>();
        }
        return storedUrls.stream()
                .map(this::refreshMediaUrl)
                .collect(Collectors.toList());
    }

    @Override
    public String refreshMediaUrl(final String storedUrl) {
        return refreshPresignedUrl(storedUrl);
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

