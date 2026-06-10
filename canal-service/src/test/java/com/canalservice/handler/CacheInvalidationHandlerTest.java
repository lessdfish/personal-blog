package com.canalservice.handler;

import com.blogcommon.cache.CacheInvalidationMessage;
import com.blogcommon.cache.CacheInvalidationPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CacheInvalidationHandlerTest {
    @Test
    void handleShouldResolveArticleKeysFromArticleTableChange() {
        CacheInvalidationPublisher publisher = mock(CacheInvalidationPublisher.class);
        CacheInvalidationHandler handler = new CacheInvalidationHandler(publisher);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redisTemplate);
        CacheInvalidationMessage message = new CacheInvalidationMessage();
        message.setTableName("tb_article");
        message.setPrimaryKey("9");
        message.setColumns(Map.of("id", "9"));

        handler.handle(message);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(publisher).publish(captor.capture());
        assertTrue(captor.getValue().contains("blog:article:detail:9"));
        assertTrue(captor.getValue().contains("blog:article:hot:"));
        verify(redisTemplate).delete(anyCollection());
    }

    @Test
    void handleShouldResolveNotifyUnreadKey() {
        CacheInvalidationPublisher publisher = mock(CacheInvalidationPublisher.class);
        CacheInvalidationHandler handler = new CacheInvalidationHandler(publisher);
        CacheInvalidationMessage message = new CacheInvalidationMessage();
        message.setTableName("tb_notify");
        message.setColumns(Map.of("user_id", "7"));

        handler.handle(message);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(publisher).publish(captor.capture());
        assertTrue(captor.getValue().contains("blog:notify:unread:7"));
    }
}
