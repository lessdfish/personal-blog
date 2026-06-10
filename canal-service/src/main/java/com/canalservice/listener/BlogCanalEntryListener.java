package com.canalservice.listener;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.blogcommon.cache.CacheInvalidationMessage;
import com.canalservice.config.CanalClientProperties;
import com.canalservice.handler.CacheInvalidationHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Blog 数据库 Canal 监听器：接收指定表的 insert/update/delete 事件并转换成缓存失效请求。
 */
@Slf4j
@Component
public class BlogCanalEntryListener {
    private final CanalClientProperties properties;
    private final CacheInvalidationHandler cacheInvalidationHandler;
    private CanalConnector connector;
    private boolean connected;

    public BlogCanalEntryListener(CanalClientProperties properties,
                                  CacheInvalidationHandler cacheInvalidationHandler) {
        this.properties = properties;
        this.cacheInvalidationHandler = cacheInvalidationHandler;
    }

    @Scheduled(fixedDelayString = "${canal.client.fixed-delay-ms:1000}")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        CanalConnector activeConnector = connector();
        Message message = activeConnector.getWithoutAck(properties.getBatchSize());
        long batchId = message.getId();
        if (batchId == -1 || message.getEntries().isEmpty()) {
            return;
        }
        log.info("Canal batch received, batchId={}, entries={}", batchId, message.getEntries().size());
        try {
            for (CanalEntry.Entry entry : message.getEntries()) {
                handleEntry(entry);
            }
            activeConnector.ack(batchId);
        } catch (Exception e) {
            activeConnector.rollback(batchId);
            log.warn("Canal batch rollback, batchId={}", batchId, e);
        }
    }

    @PreDestroy
    public void close() {
        if (connector != null && connected) {
            connector.disconnect();
        }
    }

    private CanalConnector connector() {
        if (connector == null) {
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(properties.getHost(), properties.getPort()),
                    properties.getDestination(),
                    properties.getUsername(),
                    properties.getPassword()
            );
        }
        if (!connected) {
            connector.connect();
            connector.subscribe(properties.getFilter());
            connector.rollback();
            connected = true;
            log.info("Canal connector subscribed, host={}, port={}, destination={}, filter={}",
                    properties.getHost(), properties.getPort(), properties.getDestination(), properties.getFilter());
        }
        return connector;
    }

    private void handleEntry(CanalEntry.Entry entry) throws Exception {
        if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
            return;
        }
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        String tableName = entry.getHeader().getTableName();
        if (!properties.getSupportedTables().contains(tableName)) {
            return;
        }
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            CacheInvalidationMessage message = new CacheInvalidationMessage();
            message.setTableName(tableName);
            message.setEventType(rowChange.getEventType().name());
            message.setPrimaryKey(primaryKey(rowData));
            message.setReason("canal");
            message.setColumns(columns(rowData));
            cacheInvalidationHandler.handle(message);
        }
    }

    private String primaryKey(CanalEntry.RowData rowData) {
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            if (column.getIsKey()) {
                return column.getValue();
            }
        }
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            if (column.getIsKey()) {
                return column.getValue();
            }
        }
        return null;
    }

    private Map<String, String> columns(CanalEntry.RowData rowData) {
        Map<String, String> columns = new HashMap<>();
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            columns.put(column.getName(), column.getValue());
        }
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            columns.put(column.getName(), column.getValue());
        }
        return columns;
    }
}
