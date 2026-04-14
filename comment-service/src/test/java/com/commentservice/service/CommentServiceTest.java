package com.commentservice.service;

import com.blogcommon.message.MqConstants;
import com.blogcommon.result.Result;
import com.commentservice.client.ArticleClient;
import com.commentservice.client.UserClient;
import com.commentservice.dto.CommentCreateDTO;
import com.commentservice.dto.CommentPageQueryDTO;
import com.commentservice.entity.Comment;
import com.commentservice.mapper.CommentMapper;
import com.commentservice.vo.ArticleSimpleVO;
import com.commentservice.vo.PageResult;
import com.commentservice.vo.UserSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ArticleClient articleClient = mock(ArticleClient.class);
    private final UserClient userClient = mock(UserClient.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final CommentService commentService = new CommentService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commentService, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(commentService, "rabbitTemplate", rabbitTemplate);
        ReflectionTestUtils.setField(commentService, "articleClient", articleClient);
        ReflectionTestUtils.setField(commentService, "userClient", userClient);
        ReflectionTestUtils.setField(commentService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(commentService, "rateLimitEnabled", false);
    }

    @Test
    void createShouldSendNotifyMessageWhenReceiverDiffersFromAuthor() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(11L);
        dto.setContent("hello");

        ArticleSimpleVO articleSimpleVO = new ArticleSimpleVO();
        articleSimpleVO.setId(11L);
        articleSimpleVO.setAuthorId(99L);
        articleSimpleVO.setTitle("test article");
        articleSimpleVO.setAllowComment(1);

        when(articleClient.getSimpleById(11L)).thenReturn(Result.success(articleSimpleVO));
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(66L);
            return 1;
        }).when(commentMapper).insert(any(Comment.class));

        Long commentId = commentService.create(2L, dto);

        assertEquals(66L, commentId);
        verify(articleClient).updateCommentCount(11L, 1);
        verify(rabbitTemplate).convertAndSend(
                eq(MqConstants.COMMENT_NOTIFY_EXCHANGE),
                eq(MqConstants.COMMENT_NOTIFY_ROUTING_KEY),
                any(Object.class)
        );
    }

    @Test
    void createShouldRejectWhenArticleCommentClosed() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(12L);
        dto.setContent("blocked");

        ArticleSimpleVO articleSimpleVO = new ArticleSimpleVO();
        articleSimpleVO.setId(12L);
        articleSimpleVO.setAuthorId(7L);
        articleSimpleVO.setTitle("closed");
        articleSimpleVO.setAllowComment(0);

        when(articleClient.getSimpleById(12L)).thenReturn(Result.success(articleSimpleVO));

        assertThrows(com.blogcommon.exception.BusinessException.class,
                () -> commentService.create(2L, dto));
    }

    @Test
    void createShouldNormalizeReplyToChildAsSameLevelReply() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(15L);
        dto.setParentId(200L);
        dto.setContent("reply child");

        ArticleSimpleVO articleSimpleVO = new ArticleSimpleVO();
        articleSimpleVO.setId(15L);
        articleSimpleVO.setAuthorId(7L);
        articleSimpleVO.setTitle("thread");
        articleSimpleVO.setAllowComment(1);

        Comment child = new Comment();
        child.setId(200L);
        child.setArticleId(15L);
        child.setUserId(9L);
        child.setParentId(100L);

        when(articleClient.getSimpleById(15L)).thenReturn(Result.success(articleSimpleVO));
        when(commentMapper.selectById(200L)).thenReturn(child);
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            assertEquals(100L, comment.getParentId());
            assertEquals(9L, comment.getNotifyUserId());
            comment.setId(88L);
            return 1;
        }).when(commentMapper).insert(any(Comment.class));

        Long commentId = commentService.create(3L, dto);

        assertEquals(88L, commentId);
    }

    @Test
    void deleteShouldAllowManagerDeleteAnyComment() {
        Comment comment = new Comment();
        comment.setId(77L);
        comment.setArticleId(10L);
        comment.setUserId(99L);
        when(commentMapper.selectById(77L)).thenReturn(comment);
        when(commentMapper.deleteById(77L)).thenReturn(1);

        commentService.delete(1L, "ADMIN", 77L);

        verify(commentMapper).deleteById(77L);
        verify(articleClient).updateCommentCount(10L, -1);
    }

    @Test
    void pageByArticleShouldReturnEmptyWhenNoRootComments() {
        CommentPageQueryDTO dto = new CommentPageQueryDTO();
        dto.setArticleId(5L);
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(commentMapper.countRootCommentsByArticleId(5L)).thenReturn(0L);

        PageResult<?> result = commentService.pageByArticle(dto);

        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getList().size());
    }

    @Test
    void getRemainingCommentsShouldSubtractRedisCounter() {
        ReflectionTestUtils.setField(commentService, "rateLimitThreshold", 10);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:limit:comment:3")).thenReturn("4");

        assertEquals(6, commentService.getRemainingComments(3L));
    }
}
