package com.articleservice.converter;

import com.articleservice.entity.Article;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleConverterTest {

    @Test
    void toArticleListVOShouldRoundHeatScoreToTwoDecimals() {
        ArticleListVO vo = ArticleConverter.toArticleListVO(article(), null, 12.345D);

        assertEquals(12.35D, vo.getHeatScore());
    }

    @Test
    void toArticleDetailVOShouldRoundHeatScoreToTwoDecimals() {
        ArticleDetailVO vo = ArticleConverter.toArticleDetailVO(article(), null, 4.6653455699527795D);

        assertEquals(4.67D, vo.getHeatScore());
    }

    private Article article() {
        Article article = new Article();
        article.setId(1L);
        article.setTitle("title");
        article.setContent("content");
        article.setAuthorId(2L);
        article.setViewCount(0);
        article.setCommentCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsEssence(0);
        article.setAllowComment(1);
        return article;
    }
}
