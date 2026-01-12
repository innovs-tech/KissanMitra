package com.kissanmitra.service;

import com.kissanmitra.service.impl.MediaUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for MediaUploadService.
 *
 * <p>Tests file validation, upload, and media management.
 */
@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private MediaUploadServiceImpl mediaUploadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaUploadService, "s3Bucket", "test-bucket");
        ReflectionTestUtils.setField(mediaUploadService, "s3Region", "ap-south-1");
        ReflectionTestUtils.setField(mediaUploadService, "s3Client", s3Client);
        
        // Mock S3 putObject to return a response (we're testing service logic, not actual S3 uploads)
        // Using lenient() since some tests don't call upload (they test validation)
        lenient().doReturn(null).when(s3Client).putObject(
                any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class)
        );
    }

    @Test
    void testUploadMedia_Success() {
        // Given
        MultipartFile file1 = new MockMultipartFile(
                "files", "image.jpg", "image/jpeg", "test image content".getBytes());
        MultipartFile file2 = new MockMultipartFile(
                "files", "video.mp4", "video/mp4", "test video content".getBytes());

        // When
        List<String> urls = mediaUploadService.uploadMedia("device-id", new MultipartFile[]{file1, file2});

        // Then
        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertTrue(urls.get(0).contains("device-id"));
        // URL format: https://test-bucket.s3.ap-south-1.amazonaws.com/devices/device-id/{timestamp}-{uuid}.{ext}
        assertTrue(urls.get(0).startsWith("https://"));
        assertTrue(urls.get(0).contains("devices/"));
        assertTrue(urls.get(0).contains(".jpg") || urls.get(0).contains(".jpeg"));
    }

    @Test
    void testUploadMedia_EmptyFiles() {
        // Given
        MultipartFile[] emptyFiles = new MultipartFile[0];

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaUploadService.uploadMedia("device-id", emptyFiles);
        });
    }

    @Test
    void testUploadMedia_TooManyFiles() {
        // Given
        MultipartFile[] tooManyFiles = new MultipartFile[21]; // Max is 20
        for (int i = 0; i < 21; i++) {
            tooManyFiles[i] = new MockMultipartFile(
                    "files", "image" + i + ".jpg", "image/jpeg", "content".getBytes());
        }

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaUploadService.uploadMedia("device-id", tooManyFiles);
        });
    }

    @Test
    void testUploadMedia_FileTooLarge() {
        // Given
        byte[] largeContent = new byte[21 * 1024 * 1024]; // 21MB (max is 20MB)
        MultipartFile largeFile = new MockMultipartFile(
                "files", "large.jpg", "image/jpeg", largeContent);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaUploadService.uploadMedia("device-id", new MultipartFile[]{largeFile});
        });
    }

    @Test
    void testUploadMedia_UnsupportedFormat() {
        // Given
        MultipartFile unsupportedFile = new MockMultipartFile(
                "files", "document.pdf", "application/pdf", "content".getBytes());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaUploadService.uploadMedia("device-id", new MultipartFile[]{unsupportedFile});
        });
    }

    @Test
    void testUploadMedia_ValidFormats() {
        // Given - Test all supported formats
        MultipartFile jpg = new MockMultipartFile("files", "image.jpg", "image/jpeg", "content".getBytes());
        MultipartFile png = new MockMultipartFile("files", "image.png", "image/png", "content".getBytes());
        MultipartFile mp4 = new MockMultipartFile("files", "video.mp4", "video/mp4", "content".getBytes());
        MultipartFile mov = new MockMultipartFile("files", "video.mov", "video/quicktime", "content".getBytes());

        // When
        List<String> urls = mediaUploadService.uploadMedia("device-id",
                new MultipartFile[]{jpg, png, mp4, mov});

        // Then
        assertEquals(4, urls.size());
    }

    @Test
    void testDeleteMedia() {
        // When - Should not throw exception
        assertDoesNotThrow(() -> {
            mediaUploadService.deleteMedia("device-id", "https://test-bucket.s3.ap-south-1.amazonaws.com/devices/device-id/file.jpg");
        });
    }

    @Test
    void testSetPrimaryMedia() {
        // When - Should not throw exception
        assertDoesNotThrow(() -> {
            mediaUploadService.setPrimaryMedia("device-id", "https://test-bucket.s3.ap-south-1.amazonaws.com/devices/device-id/file.jpg");
        });
    }
}

