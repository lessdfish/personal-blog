package com.notifyservice.service;

import com.blogcommon.message.CommentNotifyMessage;
import com.notifyservice.entity.Notify;
import com.notifyservice.mapper.NotifyMapper;
import com.notifyservice.dto.NotifyPageQueryDTO;
import com.notifyservice.vo.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotifyServiceTest {
    private final NotifyMapper notifyMapper = mock(NotifyMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final NotifyService notifyService = new NotifyService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifyService, "notifyMapper", notifyMapper);
        ReflectionTestUtils.setField(notifyService, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    void handleCommentNotifyShouldInsertNotifyAndIncrementCache() {
        CommentNotifyMessage message = new CommentNotifyMessage();
        message.setReceiverId(8L);
        message.setSenderId(2L);
        message.setArticleId(6L);
        message.setCommentId(99L);
        message.setArticleTitle("article");
        message.setContent("content");

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        notifyService.handleCommentNotify(message);

        verify(notifyMapper).insert(any(Notify.class));
        verify(valueOperations).increment("blog:notify:unread:8");
        verify(stringRedisTemplate).expire("blog:notify:unread:8", 300L, TimeUnit.SECONDS);
    }

    @Test
    void getUnreadCountShouldReadCacheFirst() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:notify:unread:3")).thenReturn("5");

        assertEquals(5L, notifyService.getUnreadCount(3L));
    }

    @Test
    void getUnreadCountShouldFallbackToDatabaseAndWriteCache() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:notify:unread:4")).thenReturn(null);
        when(notifyMapper.countUnreadByUserId(4L)).thenReturn(2L);

        assertEquals(2L, notifyService.getUnreadCount(4L));
        verify(valueOperations).set("blog:notify:unread:4", "2", 300L, TimeUnit.SECONDS);
    }

    @Test
    void pageByUserShouldReturnEmptyWhenNoData() {
        NotifyPageQueryDTO dto = new NotifyPageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(notifyMapper.countByUserId(5L)).thenReturn(0L);

        PageResult<?> result = notifyService.pageByUser(5L, dto);

        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getList().size());
    }

    @Test
    void markAsReadShouldDecrementUnreadCache() {
        Notify notify = new Notify();
        notify.setId(9L);
        notify.setUserId(6L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(notifyMapper.selectById(9L)).thenReturn(notify);
        when(notifyMapper.markAsRead(9L, 6L)).thenReturn(1);

        notifyService.markAsRead(6L, 9L);

        verify(valueOperations).decrement("blog:notify:unread:6", 1);
    }

    @Test
    void deleteShouldRejectCrossUserOperation() {
        Notify notify = new Notify();
        notify.setId(10L);
        notify.setUserId(99L);
        when(notifyMapper.selectById(10L)).thenReturn(notify);

        assertThrows(com.blogcommon.exception.BusinessException.class,
                () -> notifyService.delete(1L, 10L));
    }
}
