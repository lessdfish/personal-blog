package com.blogcommon.cache;

import java.util.Map;

/**
 * 缓存失效消息：描述 Canal 或业务写操作触发的表名、主键、业务缓存类型和失效原因。
 */
public class CacheInvalidationMessage {
    private String tableName;
    private String eventType;
    private String primaryKey;
    private String reason;
    private Map<String, String> columns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, String> columns) {
        this.columns = columns;
    }
}
