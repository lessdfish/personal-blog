package com.articleservice.service;

import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleFavoriteMapper;
import com.articleservice.mapper.ArticleLikeMapper;
import com.articleservice.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {
    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleLikeMapper articleLikeMapper = mock(ArticleLikeMapper.class);
    private final ArticleFavoriteMapper articleFavoriteMapper = mock(ArticleFavoriteMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final ArticleService articleService = new ArticleService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(articleService, "articleMapper", articleMapper);
        ReflectionTestUtils.setField(articleService, "articleLikeMapper", articleLikeMapper);
        ReflectionTestUtils.setField(articleService, "articleFavoriteMapper", articleFavoriteMapper);
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(articleService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
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
    void setArticleLikeStatusShouldInsertAndSyncExactCount() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(12L);
        article.setAuthorId(2L);
        when(articleMapper.selectById(12L)).thenReturn(article);
        when(articleLikeMapper.insertIgnore(any())).thenReturn(1);
        when(articleLikeMapper.countByArticle(12L)).thenReturn(4L);

        boolean liked = articleService.setArticleLikeStatus(3L, 12L, true);

        assertTrue(liked);
        verify(articleMapper).updateLikeCountTo(12L, 4);
    }

    @Test
    void setArticleFavoriteStatusShouldDeleteAndSyncExactCount() {
        ReflectionTestUtils.setField(articleService, "stringRedisTemplate", null);
        Article article = new Article();
        article.setId(16L);
        article.setAuthorId(2L);
        when(articleMapper.selectById(16L)).thenReturn(article);
        when(articleFavoriteMapper.delete(16L, 3L)).thenReturn(1);
        when(articleFavoriteMapper.countByArticle(16L)).thenReturn(2L);

        boolean favorited = articleService.setArticleFavoriteStatus(3L, 16L, false);

        assertFalse(favorited);
        verify(articleMapper).updateFavoriteCountTo(16L, 2);
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
}
