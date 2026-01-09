package com.kissanmitra.service.impl;

import com.kissanmitra.service.DocumentUploadService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
public class DocumentUploadServiceImpl implements DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_FORMATS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final int MAX_TOTAL_FILES = 10;

    @Value("${aws.s3.bucket:kissan-mitra}")
    private String s3Bucket;

    @Value("${aws.s3.region:ap-south-1}")
    private String s3Region;

    @Value("${aws.s3.accessKey:}")
    private String accessKey;

    @Value("${aws.s3.secretKey:}")
    private String secretKey;

    private S3Client s3Client;

    /**
     * Initializes S3 client on application startup.
     *
     * <p>Business Decision:
     * - Creates S3Client only if credentials are provided
     * - Logs warning if credentials are missing
     */
    @PostConstruct
    public void initS3Client() {
        if (accessKey != null && !accessKey.isEmpty() 
            && secretKey != null && !secretKey.isEmpty()) {
            try {
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                this.s3Client = S3Client.builder()
                        .region(Region.of(s3Region))
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();
                log.info("S3 client initialized successfully for bucket: {} in region: {}", s3Bucket, s3Region);
            } catch (Exception e) {
                log.error("Failed to initialize S3 client: {}", e.getMessage(), e);
            }
        } else {
            log.warn("AWS S3 credentials not configured. S3 uploads will not work.");
        }
    }

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
        try {
            // Generate unique file name
            final String timestamp = String.valueOf(Instant.now().toEpochMilli());
            final String originalFilename = file.getOriginalFilename();
            final String extension = getFileExtension(originalFilename);
            final String uniqueFilename = String.format("%s-%s.%s",
                    timestamp, UUID.randomUUID().toString().substring(0, 8), extension);
            // BUSINESS DECISION: Store documents under documents/ prefix
            final String s3Key = String.format("documents/%s/%s/%s", entityType, entityId, uniqueFilename);

            // Upload to S3 if client is initialized
            if (s3Client != null) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                final String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                        s3Bucket, s3Region, s3Key);

                log.info("Successfully uploaded document to S3: {}", s3Url);
                return s3Url;
            } else {
                // Fallback: return placeholder URL if S3 not configured
                final String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                        s3Bucket, s3Region, s3Key);
                log.warn("S3 client not initialized. Generated placeholder URL: {}", s3Url);
                throw new RuntimeException("S3 client not initialized. Please check AWS credentials.");
            }
        } catch (final Exception e) {
            log.error("Error uploading file for {}/{}: {}", entityType, entityId, e.getMessage(), e);
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

