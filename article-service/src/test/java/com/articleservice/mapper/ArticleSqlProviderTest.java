package com.articleservice.mapper;

import com.articleservice.vo.ArticlePageQueryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleSqlProviderTest {
    @Test
    void pageQueryShouldNotSelectContentColumn() {
        ArticlePageQueryDTO dto = new ArticlePageQueryDTO();

        String sql = new ArticleSqlProvider().buildPageQuery(dto).toLowerCase();

        assertTrue(sql.contains("select id, title, summary"));
        assertFalse(sql.contains(" content"));
        assertFalse(sql.contains("*"));
    }

    @Test
    void keywordFallbackShouldNotSelectContentColumn() {
        ArticlePageQueryDTO dto = new ArticlePageQueryDTO();
        dto.setKeyword("java");

        String sql = new ArticleSqlProvider().buildPageKeywordFallbackQuery(dto).toLowerCase();

        assertTrue(sql.contains("title like"));
        assertFalse(sql.contains(" content"));
        assertFalse(sql.contains("*"));
    }
}
