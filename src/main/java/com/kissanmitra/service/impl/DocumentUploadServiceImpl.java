package com.kissanmitra.service.impl;

import com.kissanmitra.service.DocumentUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of DocumentUploadService.
 *
 * <p>Business Context:
 * - Uploads document files (PDFs, images) to AWS S3
 * - Validates file size and format
 * - Generates unique file names
 *
 * <p>Uber Logic:
 * - Validates file size and format
 * - Generates unique file names
 * - Uploads files to S3 under documents/ prefix and returns URLs
 */
@Slf4j
@Service
public class DocumentUploadServiceImpl extends BaseS3UploadService implements DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_FORMATS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final int MAX_TOTAL_FILES = 10;

    @Override
    public List<String> uploadDocuments(final String entityType, final String entityId, final MultipartFile[] files) {
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
            final String url = uploadFile(entityType, entityId, file);
            uploadedUrls.add(url);
        }

        log.info("Uploaded {} document files for {}/{}", uploadedUrls.size(), entityType, entityId);
        return uploadedUrls;
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
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file format: %s. Allowed: %s",
                            extension, String.join(", ", ALLOWED_FORMATS))
            );
        }
    }

    /**
     * Uploads file to S3 and returns URL.
     *
     * <p>Business Decision:
     * - Uploads files to S3 using AWS SDK
     * - Stores files under documents/ prefix for organization
     * - Returns S3 URL for uploaded file
     *
     * @param entityType entity type (e.g., "leases", "operators")
     * @param entityId entity ID
     * @param file file to upload
     * @return S3 URL
     */
    private String uploadFile(final String entityType, final String entityId, final MultipartFile file) {
        final String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        final String uniqueFilename = generateUniqueFilename(originalFilename);
        // BUSINESS DECISION: Store documents under documents/ prefix
        final String s3Key = String.format("documents/%s/%s/%s", entityType, entityId, uniqueFilename);

        return uploadFileToS3(s3Key, file);
    }
}

