package com.kissanmitra.service;

import com.kissanmitra.service.impl.DocumentUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DocumentUploadService.
 *
 * <p>Tests file validation, upload, and document management.
 */
@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DocumentUploadServiceImpl documentUploadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentUploadService, "s3Bucket", "test-bucket");
        ReflectionTestUtils.setField(documentUploadService, "s3Region", "ap-south-1");
        ReflectionTestUtils.setField(documentUploadService, "s3Client", s3Client);

        // Mock S3Client behavior for putObject
        lenient().when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("mock-etag").build());
    }

    @Test
    void testUploadDocuments_Success() {
        // Given
        MultipartFile file1 = new MockMultipartFile(
                "files", "document.pdf", "application/pdf", "test pdf content".getBytes());
        MultipartFile file2 = new MockMultipartFile(
                "files", "image.jpg", "image/jpeg", "test image content".getBytes());

        // When
        List<String> urls = documentUploadService.uploadDocuments("leases", "lease-id",
                new MultipartFile[]{file1, file2});

        // Then
        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertTrue(urls.get(0).contains("documents/leases/lease-id")); // Verify "documents/" prefix
        assertTrue(urls.get(0).startsWith("https://"));
        assertTrue(urls.get(0).contains(".pdf") || urls.get(0).contains(".jpg"));
    }

    @Test
    void testUploadDocuments_EmptyFiles() {
        // Given
        MultipartFile[] emptyFiles = new MultipartFile[0];

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", emptyFiles);
        });
    }

    @Test
    void testUploadDocuments_TooManyFiles() {
        // Given
        MultipartFile[] tooManyFiles = new MultipartFile[11]; // Max is 10
        for (int i = 0; i < 11; i++) {
            tooManyFiles[i] = new MockMultipartFile(
                    "files", "document" + i + ".pdf", "application/pdf", "content".getBytes());
        }

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", tooManyFiles);
        });
    }

    @Test
    void testUploadDocuments_FileTooLarge() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB (max is 10MB)
        MultipartFile largeFile = new MockMultipartFile(
                "files", "large.pdf", "application/pdf", largeContent);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", new MultipartFile[]{largeFile});
        });
    }

    @Test
    void testUploadDocuments_UnsupportedFormat() {
        // Given
        MultipartFile unsupportedFile = new MockMultipartFile(
                "files", "document.doc", "application/msword", "content".getBytes());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", new MultipartFile[]{unsupportedFile});
        });
    }

    @Test
    void testUploadDocuments_ValidFormats() {
        // Given - Test all supported formats
        MultipartFile pdf = new MockMultipartFile("files", "doc.pdf", "application/pdf", "content".getBytes());
        MultipartFile jpg = new MockMultipartFile("files", "image.jpg", "image/jpeg", "content".getBytes());
        MultipartFile jpeg = new MockMultipartFile("files", "image.jpeg", "image/jpeg", "content".getBytes());
        MultipartFile png = new MockMultipartFile("files", "image.png", "image/png", "content".getBytes());

        // When
        List<String> urls = documentUploadService.uploadDocuments("operators", "operator-id",
                new MultipartFile[]{pdf, jpg, jpeg, png});

        // Then
        assertEquals(4, urls.size());
        assertTrue(urls.get(0).contains("documents/operators/operator-id")); // Verify "documents/" prefix
    }

    @Test
    void testUploadDocuments_EmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "files", "empty.pdf", "application/pdf", new byte[0]);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", new MultipartFile[]{emptyFile});
        });
    }

    @Test
    void testUploadDocuments_NullFiles() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            documentUploadService.uploadDocuments("leases", "lease-id", null);
        });
    }
}

