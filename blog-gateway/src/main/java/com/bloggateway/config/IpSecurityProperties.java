package com.bloggateway.config;

import com.blogcommon.config.RefreshablePropertiesMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "security.ip")
public class IpSecurityProperties implements RefreshablePropertiesMarker {
    private List<String> trustedProxies = new ArrayList<>(List.of("127.0.0.1", "::1"));
    private Blocklist blocklist = new Blocklist();

    /**
     * 获取 trustedProxies：返回当前对象里保存的这个值。
     */
    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    /**
     * 设置 trustedProxies：把外部传入的值保存到当前对象。
     */
    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    /**
     * 获取 blocklist：返回当前对象里保存的这个值。
     */
    public Blocklist getBlocklist() {
        return blocklist;
    }

    /**
     * 设置 blocklist：把外部传入的值保存到当前对象。
     */
    public void setBlocklist(Blocklist blocklist) {
        this.blocklist = blocklist;
    }

    public static class Blocklist {
        private boolean enabled = true;
        private long ttlSeconds = 600;

        /**
         * 判断 enabled：返回当前开关或状态是否成立。
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 enabled：把外部传入的值保存到当前对象。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 ttlSeconds：返回当前对象里保存的这个值。
         */
        public long getTtlSeconds() {
            return ttlSeconds;
        }

        /**
         * 设置 ttlSeconds：把外部传入的值保存到当前对象。
         */
        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
