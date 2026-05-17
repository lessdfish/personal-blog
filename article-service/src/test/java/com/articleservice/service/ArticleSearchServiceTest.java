package com.articleservice.service;

import com.articleservice.entity.Article;
import com.articleservice.entity.ArticleDocument;
import com.articleservice.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleSearchServiceTest {
    private final ElasticsearchOperations elasticsearchOperations = mock(ElasticsearchOperations.class);
    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleSearchService articleSearchService = new ArticleSearchService(elasticsearchOperations, articleMapper);

    @Test
    void reindexAllShouldPageThroughActiveArticles() {
        when(articleMapper.selectActiveForSearchReindex(0, 2)).thenReturn(List.of(article(1L), article(2L)));
        when(articleMapper.selectActiveForSearchReindex(2, 2)).thenReturn(List.of(article(3L)));

        ArticleSearchService.ReindexResult result = articleSearchService.reindexAll(2);

        assertEquals(3L, result.indexedCount());
        assertEquals(2, result.pageSize());
        verify(articleMapper).selectActiveForSearchReindex(0, 2);
        verify(articleMapper).selectActiveForSearchReindex(2, 2);
        verify(elasticsearchOperations, times(3)).save(any(ArticleDocument.class));
    }

    @Test
    void reindexAllShouldCapPageSize() {
        when(articleMapper.selectActiveForSearchReindex(0, 500)).thenReturn(List.of());

        ArticleSearchService.ReindexResult result = articleSearchService.reindexAll(5000);

        assertEquals(0L, result.indexedCount());
        assertEquals(500, result.pageSize());
        verify(articleMapper).selectActiveForSearchReindex(0, 500);
    }

    private Article article(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("title " + id);
        article.setSummary("summary " + id);
        article.setContent("content " + id);
        article.setAuthorId(1L);
        article.setBoardId(1L);
        article.setTags("tag");
        article.setStatus(1);
        article.setIsEssence(0);
        return article;
    }
}
