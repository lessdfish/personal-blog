package com.userservice.service;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.userservice.config.AvatarUploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvatarStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void storeShouldAcceptValidJpegAndReturnPublicPath() {
        AvatarStorageService service = new AvatarStorageService(properties(1024));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00}
        );

        String path = service.store(7L, file);

        assertTrue(path.matches("/api/user/avatar/7-[a-f0-9]{32}\\.jpg"));
        String filename = path.substring(path.lastIndexOf('/') + 1);
        assertTrue(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    void storeShouldRejectBadExtension() {
        AvatarStorageService service = new AvatarStorageService(properties(1024));
        MockMultipartFile file = jpegFile("avatar.txt", "image/jpeg");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.store(7L, file));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertNoStoredFiles();
    }

    @Test
    void storeShouldRejectDoubleExtension() {
        AvatarStorageService service = new AvatarStorageService(properties(1024));
        MockMultipartFile file = jpegFile("avatar.jpg.exe", "image/jpeg");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.store(7L, file));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertNoStoredFiles();
    }

    @Test
    void storeShouldRejectBadMimeType() {
        AvatarStorageService service = new AvatarStorageService(properties(1024));
        MockMultipartFile file = jpegFile("avatar.jpg", "text/plain");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.store(7L, file));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertNoStoredFiles();
    }

    @Test
    void storeShouldRejectBadSignature() {
        AvatarStorageService service = new AvatarStorageService(properties(1024));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "<html></html>".getBytes()
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> service.store(7L, file));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertNoStoredFiles();
    }

    @Test
    void storeShouldRejectOversizeBeforeWritingFile() {
        AvatarStorageService service = new AvatarStorageService(properties(3));
        MockMultipartFile file = jpegFile("avatar.jpg", "image/jpeg");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.store(7L, file));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertNoStoredFiles();
    }

    private AvatarUploadProperties properties(long maxSizeBytes) {
        AvatarUploadProperties properties = new AvatarUploadProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setMaxSizeBytes(maxSizeBytes);
        return properties;
    }

    private MockMultipartFile jpegFile(String filename, String contentType) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00}
        );
    }

    private void assertNoStoredFiles() {
        if (!Files.exists(tempDir)) {
            return;
        }
        try (var files = Files.list(tempDir)) {
            assertEquals(0, files.count());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
