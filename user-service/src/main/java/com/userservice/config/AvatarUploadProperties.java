package com.userservice.config;

import com.blogcommon.config.RefreshablePropertiesMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.avatar")
public class AvatarUploadProperties implements RefreshablePropertiesMarker {
    private String uploadDir;
    private long maxSizeBytes = 5L * 1024 * 1024;
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp", "gif");
    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    /**
     * 获取 uploadDir：返回当前对象里保存的这个值。
     */
    public String getUploadDir() {
        return uploadDir;
    }

    /**
     * 设置 uploadDir：把外部传入的值保存到当前对象。
     */
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 获取 maxSizeBytes：返回当前对象里保存的这个值。
     */
    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * 设置 maxSizeBytes：把外部传入的值保存到当前对象。
     */
    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * 获取 allowedExtensions：返回当前对象里保存的这个值。
     */
    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    /**
     * 设置 allowedExtensions：把外部传入的值保存到当前对象。
     */
    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    /**
     * 获取 allowedContentTypes：返回当前对象里保存的这个值。
     */
    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    /**
     * 设置 allowedContentTypes：把外部传入的值保存到当前对象。
     */
    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
