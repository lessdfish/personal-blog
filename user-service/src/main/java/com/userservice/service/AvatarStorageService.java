package com.userservice.service;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.userservice.config.AvatarUploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private final AvatarUploadProperties properties;

    /**
     * 构造 AvatarStorageService：注入这个类运行时需要的依赖。
     */
    public AvatarStorageService(AvatarUploadProperties properties) {
        this.properties = properties;
    }

    /**
     * 业务方法 store：封装 AvatarStorageService 中对应的核心处理流程。
     */
    public String store(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择头像文件");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "头像大小不能超过5MB");
        }

        AvatarType type = validateType(file);
        String filename = userId + "-" + UUID.randomUUID().toString().replace("-", "") + "." + type.extension();
        Path uploadDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "头像文件名不合法");
        }

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "头像上传失败");
        }
        return "/api/user/avatar/" + filename;
    }

    /**
     * 业务方法 validateType：封装 AvatarStorageService 中对应的核心处理流程。
     */
    private AvatarType validateType(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw unsupportedType();
        }
        AvatarType extensionType = AvatarType.fromExtension(extension);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!properties.getAllowedContentTypes().contains(contentType)) {
            throw unsupportedType();
        }
        AvatarType mimeType = AvatarType.fromContentType(contentType);
        AvatarType signatureType = detectSignature(file);
        if (extensionType == null || mimeType == null || signatureType == null
                || extensionType != mimeType || extensionType != signatureType) {
            throw unsupportedType();
        }
        return signatureType;
    }

    /**
     * 获取 extension：返回当前对象里保存的这个值。
     */
    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw unsupportedType();
        }
        String filename = Path.of(originalFilename).getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            throw unsupportedType();
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 业务方法 detectSignature：封装 AvatarStorageService 中对应的核心处理流程。
     */
    private AvatarType detectSignature(MultipartFile file) {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(16);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "头像读取失败");
        }
        return AvatarType.fromHeader(header);
    }

    /**
     * 业务方法 unsupportedType：封装 AvatarStorageService 中对应的核心处理流程。
     */
    private BusinessException unsupportedType() {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), "仅支持 jpg、jpeg、png、webp、gif 格式头像");
    }

    private enum AvatarType {
        JPEG("jpg", Set.of("jpg", "jpeg"), Set.of("image/jpeg")),
        PNG("png", Set.of("png"), Set.of("image/png")),
        GIF("gif", Set.of("gif"), Set.of("image/gif")),
        WEBP("webp", Set.of("webp"), Set.of("image/webp"));

        private final String extension;
        private final Set<String> extensions;
        private final Set<String> contentTypes;

        AvatarType(String extension, Set<String> extensions, Set<String> contentTypes) {
            this.extension = extension;
            this.extensions = extensions;
            this.contentTypes = contentTypes;
        }

        /**
         * 业务方法 extension：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private String extension() {
            return extension;
        }

        /**
         * 业务方法 fromExtension：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static AvatarType fromExtension(String extension) {
            for (AvatarType type : values()) {
                if (type.extensions.contains(extension)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * 业务方法 fromContentType：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static AvatarType fromContentType(String contentType) {
            for (AvatarType type : values()) {
                if (type.contentTypes.contains(contentType)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * 业务方法 fromHeader：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static AvatarType fromHeader(byte[] header) {
            if (startsWith(header, bytes(0xff, 0xd8, 0xff))) {
                return JPEG;
            }
            if (startsWith(header, bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))) {
                return PNG;
            }
            if (startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
                return GIF;
            }
            if (header.length >= 12
                    && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && matchesAt(header, 8, "WEBP".getBytes(StandardCharsets.US_ASCII))) {
                return WEBP;
            }
            return null;
        }

        /**
         * 业务方法 bytes：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static byte[] bytes(int... values) {
            byte[] bytes = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                bytes[i] = (byte) values[i];
            }
            return bytes;
        }

        /**
         * 业务方法 startsWith：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static boolean startsWith(byte[] source, byte[] prefix) {
            return matchesAt(source, 0, prefix);
        }

        /**
         * 业务方法 matchesAt：封装 AvatarStorageService 中对应的核心处理流程。
         */
        private static boolean matchesAt(byte[] source, int offset, byte[] expected) {
            if (source.length < offset + expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (source[offset + i] != expected[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
