package com.canalservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Canal 客户端配置：声明 Canal Server 地址、destination、订阅表达式和批量消费参数。
 */
@ConfigurationProperties(prefix = "canal.client")
public class CanalClientProperties {
    private boolean enabled = true;
    private String host = "127.0.0.1";
    private int port = 11111;
    private String destination = "example";
    private String username = "";
    private String password = "";
    private String filter = "blog_cloud\\..*";
    private int batchSize = 100;
    private long fixedDelayMs = 1000;
    private List<String> supportedTables = List.of(
            "tb_article",
            "tb_board",
            "tb_user",
            "tb_role_permission",
            "tb_notify",
            "tb_article_like",
            "tb_article_favorite",
            "tb_comment"
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public List<String> getSupportedTables() {
        return supportedTables;
    }

    public void setSupportedTables(List<String> supportedTables) {
        this.supportedTables = supportedTables;
    }
}
