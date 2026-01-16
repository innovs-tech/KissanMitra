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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;

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
    protected S3Presigner s3Presigner;

    /**
     * S3 URL format template for direct URLs.
     * Format: https://{bucket}.s3.{region}.amazonaws.com/{key}
     */
    private static final String S3_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

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
            log.info("Initializing S3 client for bucket: {} in region: {}", s3Bucket, s3Region);
            log.info("Access key: {}", accessKey);
            log.info("Secret key: {}", secretKey);
            
            // BUSINESS DECISION: Use IAM roles on ECS (default provider chain)
            // Fallback to access key/secret key only for local development
            if (accessKey != null && !accessKey.isEmpty()
                    && secretKey != null && !secretKey.isEmpty()) {
                // Local development: Use explicit credentials
                log.info("Using explicit credentials for S3 (local dev mode)");
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                this.s3Client = S3Client.builder()
                        .region(Region.of(s3Region))
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();
                log.info("S3 client initialized with explicit credentials (local dev mode)");
            } else {
                // Production (ECS): Use default credential provider chain (IAM task role)
                log.info("Using default credential provider chain for S3 (IAM role on ECS)");
                this.s3Client = S3Client.builder()
                        .region(Region.of(s3Region))
                        .build();
                log.info("S3 client built using default credential provider chain (IAM role on ECS)");
            }

            // Validate client was created
            if (this.s3Client == null) {
                throw new IllegalStateException("S3Client builder returned null");
            }

            // Initialize presigner for generating presigned URLs
            if (accessKey != null && !accessKey.isEmpty()
                    && secretKey != null && !secretKey.isEmpty()) {
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                this.s3Presigner = S3Presigner.builder()
                        .region(Region.of(s3Region))
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();
            } else {
                this.s3Presigner = S3Presigner.builder()
                        .region(Region.of(s3Region))
                        .build();
            }

            log.info("S3 client initialized successfully for bucket: {} in region: {}", s3Bucket, s3Region);
        } catch (IllegalArgumentException e) {
            log.error("Invalid S3 configuration - region: {}, bucket: {}, error: {}", s3Region, s3Bucket, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 client: Invalid configuration - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to initialize S3 client - region: {}, bucket: {}, error: {}", s3Region, s3Bucket, e.getMessage(), e);
            log.error("S3 initialization failed. Check: 1) IAM role attached to ECS task, 2) IAM role has S3 permissions, 3) Region is valid: {}", s3Region);
            throw new RuntimeException("Failed to initialize S3 client: " + e.getMessage() + ". Check IAM role and permissions.", e);
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

                // BUSINESS DECISION: Generate presigned URL instead of direct URL
                // Direct URLs require public bucket access, but bucket blocks public access
                // Presigned URLs allow temporary access without making bucket public
                final String s3Url;
                if (s3Presigner != null) {
                    // Generate presigned URL (valid for 7 days)
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(s3Key)
                            .build();
                    
                    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                            presigner -> presigner
                                    .signatureDuration(Duration.ofDays(7))
                                    .getObjectRequest(getObjectRequest)
                    );
                    
                    s3Url = presignedRequest.url().toString();
                } else {
                    // Fallback to direct URL (will fail if bucket blocks public access)
                    s3Url = String.format(S3_URL_FORMAT, s3Bucket, s3Region, s3Key);
                }

                log.info("Successfully uploaded file to S3: {}", s3Url);
                return s3Url;
            } else {
                throwS3ClientNotInitializedException();
                return null; // Never reached, but required for compilation
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

    /**
     * Generates a fresh presigned URL for an S3 key.
     *
     * <p>Business Decision:
     * - Generates presigned URLs on-demand (valid for 7 days)
     * - URLs are always fresh and never expire when generated on-demand
     * - Used when returning device data to frontend
     *
     * @param s3Key S3 key (path) for the file
     * @return presigned URL
     */
    public String generatePresignedUrl(final String s3Key) {
        if (s3Presigner == null) {
            log.warn("S3 presigner not initialized, returning direct URL (may not work if bucket blocks public access)");
            return String.format(S3_URL_FORMAT, s3Bucket, s3Region, s3Key);
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    presigner -> presigner
                            .signatureDuration(Duration.ofDays(7))
                            .getObjectRequest(getObjectRequest)
            );

            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}, error: {}", s3Key, e.getMessage(), e);
            // Fallback to direct URL
            return String.format(S3_URL_FORMAT, s3Bucket, s3Region, s3Key);
        }
    }

    /**
     * Extracts S3 key from S3 URL (presigned or direct).
     *
     * <p>Business Decision:
     * - Supports both presigned URLs and direct S3 URLs
     * - Used for backward compatibility with existing stored URLs
     *
     * @param s3Url S3 URL (presigned or direct)
     * @return S3 key (path) or null if URL format is invalid
     */
    protected String extractS3KeyFromUrl(final String s3Url) {
        if (s3Url == null || s3Url.isEmpty()) {
            return null;
        }

        try {
            // Handle presigned URLs: https://bucket.s3.region.amazonaws.com/key?X-Amz-...
            // Handle direct URLs: https://bucket.s3.region.amazonaws.com/key
            final String bucketPrefix = s3Bucket + ".s3.";
            final int bucketIndex = s3Url.indexOf(bucketPrefix);
            if (bucketIndex == -1) {
                // Try alternative format: s3://bucket/key
                if (s3Url.startsWith("s3://")) {
                    final String withoutPrefix = s3Url.substring(5);
                    final int slashIndex = withoutPrefix.indexOf('/');
                    if (slashIndex != -1 && withoutPrefix.startsWith(s3Bucket + "/")) {
                        return withoutPrefix.substring(s3Bucket.length() + 1);
                    }
                }
                log.warn("Could not extract S3 key from URL: {}", s3Url);
                return null;
            }

            // Extract key from URL
            final String afterBucket = s3Url.substring(bucketIndex + bucketPrefix.length());
            final int regionEndIndex = afterBucket.indexOf(".amazonaws.com/");
            if (regionEndIndex == -1) {
                log.warn("Invalid S3 URL format: {}", s3Url);
                return null;
            }

            final String afterAmazonaws = afterBucket.substring(regionEndIndex + 15);
            // Remove query parameters if present (for presigned URLs)
            final int queryIndex = afterAmazonaws.indexOf('?');
            final String s3Key = queryIndex != -1 ? afterAmazonaws.substring(0, queryIndex) : afterAmazonaws;

            return s3Key;
        } catch (Exception e) {
            log.error("Error extracting S3 key from URL: {}, error: {}", s3Url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generates a fresh presigned URL from a stored S3 URL.
     *
     * <p>Business Decision:
     * - Extracts S3 key from stored URL (backward compatible)
     * - Generates fresh presigned URL (valid for 7 days)
     * - Used when returning device data to frontend to ensure URLs never expire
     *
     * @param storedUrl stored S3 URL (presigned or direct)
     * @return fresh presigned URL or original URL if extraction fails
     */
    public String refreshPresignedUrl(final String storedUrl) {
        if (storedUrl == null || storedUrl.isEmpty()) {
            return storedUrl;
        }

        final String s3Key = extractS3KeyFromUrl(storedUrl);
        if (s3Key == null) {
            log.warn("Could not extract S3 key from stored URL, returning original: {}", storedUrl);
            return storedUrl;
        }

        return generatePresignedUrl(s3Key);
    }

    /**
     * Throws exception when S3 client is not initialized.
     *
     * <p>Business Decision:
     * - Centralized error handling for S3 client initialization failures
     * - Provides detailed troubleshooting information
     */
    private void throwS3ClientNotInitializedException() {
        log.error("S3 client is null. This indicates initialization failed. Bucket: {}, Region: {}", s3Bucket, s3Region);
        log.error("Troubleshooting: 1) Check application startup logs for S3 initialization errors");
        log.error("2) Verify ECS task has IAM role attached (taskRoleArn)");
        log.error("3) Verify IAM role has S3 permissions for bucket: {}", s3Bucket);
        log.error("4) Check CloudWatch logs for detailed error messages");
        throw new RuntimeException(
                String.format("S3 client not initialized. Bucket: %s, Region: %s. Check IAM role and permissions.", 
                        s3Bucket, s3Region));
    }

    /**
     * Deletes a file from S3.
     *
     * <p>Business Decision:
     * - Deletes file from S3 bucket using AWS SDK
     * - Used when media files are removed from devices
     *
     * @param s3Key S3 key (path) for the file to delete
     * @throws RuntimeException if deletion fails or S3 client not initialized
     */
    protected void deleteFileFromS3(final String s3Key) {
        if (s3Client == null) {
            throwS3ClientNotInitializedException();
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3: {}", s3Key);
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}, error: {}", s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}

