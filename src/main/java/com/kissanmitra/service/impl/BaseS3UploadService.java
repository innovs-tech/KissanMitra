package com.kissanmitra.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Base abstract class for S3 file upload services.
 *
 * <p>Business Context:
 * - Provides common S3 client initialization and upload logic
 * - Uses IAM roles on ECS, falls back to access keys for local development
 * - Handles S3 bucket configuration and region settings
 *
 * <p>Uber Logic:
 * - Initializes S3 client with appropriate credentials
 * - Provides common file upload functionality
 * - Generates unique file names
 */
@Slf4j
public abstract class BaseS3UploadService {

    @Value("${aws.s3.bucket:kissan-mitra-dev}")
    protected String s3Bucket;

    @Value("${aws.s3.region:ap-south-1}")
    protected String s3Region;

    @Value("${aws.s3.accessKey:}")
    protected String accessKey;

    @Value("${aws.s3.secretKey:}")
    protected String secretKey;

    protected S3Client s3Client;

    /**
     * Initializes S3 client on application startup.
     *
     * <p>Business Decision:
     * - Uses IAM roles on ECS (default credential provider chain)
     * - Falls back to access key/secret key only for local development
     * - Automatically picks up credentials from environment, IAM roles, etc.
     */
    @PostConstruct
    public void initS3Client() {
        try {
            // BUSINESS DECISION: Use IAM roles on ECS (default provider chain)
            // Fallback to access key/secret key only for local development
            if (accessKey != null && !accessKey.isEmpty()
                    && secretKey != null && !secretKey.isEmpty()) {
                // Local development: Use explicit credentials
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                this.s3Client = S3Client.builder()
                        .region(Region.of(s3Region))
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();
                log.info("S3 client initialized with explicit credentials (local dev mode)");
            } else {
                // Production (ECS): Use default credential provider chain (IAM task role)
                this.s3Client = S3Client.builder()
                        .region(Region.of(s3Region))
                        .build();
                log.info("S3 client will use default credential provider chain (IAM role on ECS)");
            }

            log.info("S3 client initialized successfully for bucket: {} in region: {}", s3Bucket, s3Region);
        } catch (Exception e) {
            log.error("Failed to initialize S3 client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 client", e);
        }
    }

    /**
     * Uploads file to S3 and returns URL.
     *
     * <p>Business Decision:
     * - Uploads files to S3 using AWS SDK
     * - Generates unique file names with timestamp and UUID
     * - Returns S3 URL for uploaded file
     *
     * @param s3Key S3 key (path) for the file
     * @param file file to upload
     * @return S3 URL
     */
    protected String uploadFileToS3(final String s3Key, final MultipartFile file) {
        try {
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

                log.info("Successfully uploaded file to S3: {}", s3Url);
                return s3Url;
            } else {
                // Fallback: return placeholder URL if S3 not configured
                final String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                        s3Bucket, s3Region, s3Key);
                log.warn("S3 client not initialized. Generated placeholder URL: {}", s3Url);
                throw new RuntimeException("S3 client not initialized. Please check AWS credentials.");
            }
        } catch (final Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Generates unique filename with timestamp and UUID.
     *
     * @param originalFilename original file name
     * @return unique filename
     */
    protected String generateUniqueFilename(final String originalFilename) {
        final String extension = getFileExtension(originalFilename);
        final String timestamp = String.valueOf(Instant.now().toEpochMilli());
        return String.format("%s-%s.%s",
                timestamp, UUID.randomUUID().toString().substring(0, 8), extension);
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename file name
     * @return file extension (without dot)
     * @throws IllegalArgumentException if file has no extension
     */
    protected String getFileExtension(final String filename) {
        final int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1);
    }
}

