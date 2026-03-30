package com.articleservice.converter;

import com.articleservice.entity.Article;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;

/**
 * ClassName:ArticleConverter
 * Package:com.articleservice.converter
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/29 - 00:08
 * @Version: v1.0
 *
 */
public class ArticleConverter {

        public static ArticleListVO toArticleListVO(Article article) {
            if (article == null) {
                return null;
            }

            ArticleListVO vo = new ArticleListVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setAuthorId(article.getAuthorId());
            vo.setViewCount(article.getViewCount());
            vo.setCreateTime(article.getCreateTime());
            vo.setUpdateTime(article.getUpdateTime());
            return vo;
        }

    public static ArticleDetailVO toArticleDetailVO(Article article) {
        if (article == null) {
            return null;
        }

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setAuthorId(article.getAuthorId());
        vo.setViewCount(article.getViewCount());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }
    }

