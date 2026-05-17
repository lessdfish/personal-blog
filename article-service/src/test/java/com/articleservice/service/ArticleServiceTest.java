package com.articleservice.service;

import com.articleservice.dto.ArticleManageDTO;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.entity.Article;
import com.articleservice.client.UserClient;
import com.articleservice.mapper.ArticleFavoriteMapper;
import com.articleservice.mapper.ArticleLikeMapper;
import com.articleservice.mapper.ArticleMapper;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.auth.RequestUserContext;
import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.MqConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {
    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleLikeMapper articleLikeMapper = mock(ArticleLikeMapper.class);
    private final ArticleFavoriteMapper articleFavoriteMapper = mock(ArticleFavoriteMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UserClient userClient = mock(UserClient.class);
    private final ArticleService articleService = new ArticleService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(articleService, "articleMapper", articleMapper);
        ReflectionTestUtils.setField(articleService, "articleLikeMapper", articleLikeMapper);
        ReflectionTestUtils.setField(articleService, "articleFavoriteMapper", articleFavoriteMapper);
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(articleService, "rabbitTemplate", rabbitTemplate);
        ReflectionTestUtils.setField(articleService, "userClient", userClient);
        ReflectionTestUtils.setField(articleService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        RequestUserContext.clear();
    }

    @Test
    void hasLikedShouldReturnTrueWhenRedisSetContainsArticle() {
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("blog:article:liked:2", "8")).thenReturn(true);

        assertTrue(articleService.hasLiked(2L, 8L));
    }

    @Test
    void hasLikedShouldFallbackToDatabaseWhenCacheMiss() {
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("blog:article:liked:2", "9")).thenReturn(false);
        when(articleLikeMapper.countByArticleAndUser(9L, 2L)).thenReturn(1L);

        assertTrue(articleService.hasLiked(2L, 9L));
        when(articleLikeMapper.countByArticleAndUser(10L, 2L)).thenReturn(0L);
        assertFalse(articleService.hasLiked(2L, 10L));
    }

    @Test
    void setArticleLikeStatusShouldInsertAndIncrementCount() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(12L);
        article.setAuthorId(2L);
        when(articleMapper.selectById(12L)).thenReturn(article);
        when(articleLikeMapper.insertIgnore(any())).thenReturn(1);

        boolean liked = articleService.setArticleLikeStatus(3L, 12L, true);

        assertTrue(liked);
        verify(articleMapper).incrementLikeCount(12L, 1);
    }

    @Test
    void setArticleLikeStatusShouldUseRequestUsernameWithoutCallingUserService() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        RequestUserContext.setUsername("alice");
        Article article = new Article();
        article.setId(13L);
        article.setAuthorId(2L);
        article.setTitle("title");
        when(articleMapper.selectById(13L)).thenReturn(article);
        when(articleLikeMapper.insertIgnore(any())).thenReturn(1);

        articleService.setArticleLikeStatus(3L, 13L, true);

        verify(userClient, never()).getBatchUserSimple(any());
        ArgumentCaptor<ArticleInteractionNotifyMessage> captor =
                ArgumentCaptor.forClass(ArticleInteractionNotifyMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE),
                eq(MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY),
                captor.capture()
        );
        assertEquals("alice", captor.getValue().getSenderName());
    }

    @Test
    void setArticleFavoriteStatusShouldDeleteAndDecrementCount() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(16L);
        article.setAuthorId(2L);
        when(articleMapper.selectById(16L)).thenReturn(article);
        when(articleFavoriteMapper.delete(16L, 3L)).thenReturn(1);

        boolean favorited = articleService.setArticleFavoriteStatus(3L, 16L, false);

        assertFalse(favorited);
        verify(articleMapper).incrementFavoriteCount(16L, -1);
    }

    @Test
    void updateArticleCommentCountShouldProtectLowerBound() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(18L);
        article.setCommentCount(0);
        when(articleMapper.selectAnyById(18L)).thenReturn(article);
        when(articleMapper.updateCommentCountTo(18L, 0)).thenReturn(1);

        articleService.updateArticleCommentCount(18L, -1);

        verify(articleMapper).updateCommentCountTo(18L, 0);
    }

    @Test
    void updateArticleCommentCountShouldThrowWhenArticleMissing() {
        when(articleMapper.selectAnyById(19L)).thenReturn(null);

        assertThrows(com.blogcommon.exception.BusinessException.class,
                () -> articleService.updateArticleCommentCount(19L, 1));
    }

    @Test
    void editArticleShouldRejectModeratorWhenNotAuthor() {
        Article article = new Article();
        article.setId(21L);
        article.setAuthorId(2L);
        when(articleMapper.selectById(21L)).thenReturn(article);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleService.editArticle(3L, "MODERATOR", 21L, validPublishDTO()));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void deleteArticleShouldAllowAuthor() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(22L);
        article.setAuthorId(3L);
        article.setStatus(1);
        article.setViewCount(0);
        article.setCommentCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsEssence(0);
        when(articleMapper.selectById(22L)).thenReturn(article);
        when(articleMapper.selectAnyById(22L)).thenReturn(article);
        when(articleMapper.updateStatus(22L, 0)).thenReturn(1);

        articleService.deleteArticle(3L, "USER", 22L);

        verify(articleMapper).updateStatus(22L, 0);
    }

    @Test
    void manageArticleShouldRejectModerator() {
        ArticleManageDTO dto = new ArticleManageDTO();
        dto.setIsTop(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleService.manageArticle("MODERATOR", 23L, dto));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    private ArticlePublishDTO validPublishDTO() {
        ArticlePublishDTO dto = new ArticlePublishDTO();
        dto.setTitle("title");
        dto.setContent("content");
        return dto;
    }
}
