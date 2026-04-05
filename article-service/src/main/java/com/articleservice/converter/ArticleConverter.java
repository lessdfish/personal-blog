package com.articleservice.converter;

import com.articleservice.entity.Article;
import com.articleservice.entity.Board;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;

public class ArticleConverter {
    public static ArticleListVO toArticleListVO(Article article, Board board, Double heatScore) {
        if (article == null) {
            return null;
        }
        ArticleListVO vo = new ArticleListVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setAuthorId(article.getAuthorId());
        vo.setBoardId(article.getBoardId());
        vo.setBoardName(board == null ? null : board.getBoardName());
        vo.setTags(article.getTags());
        vo.setViewCount(article.getViewCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setFavoriteCount(article.getFavoriteCount());
        vo.setIsTop(article.getIsTop());
        vo.setIsEssence(article.getIsEssence());
        vo.setHeatScore(heatScore);
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }

    public static ArticleDetailVO toArticleDetailVO(Article article, Board board, Double heatScore) {
        if (article == null) {
            return null;
        }
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setAuthorId(article.getAuthorId());
        vo.setBoardId(article.getBoardId());
        vo.setBoardName(board == null ? null : board.getBoardName());
        vo.setTags(article.getTags());
        vo.setViewCount(article.getViewCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setFavoriteCount(article.getFavoriteCount());
        vo.setIsTop(article.getIsTop());
        vo.setIsEssence(article.getIsEssence());
        vo.setAllowComment(article.getAllowComment());
        vo.setHeatScore(heatScore);
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }
}
