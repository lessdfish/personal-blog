package com.articleservice.converter;

import com.articleservice.entity.Article;
import com.articleservice.entity.Board;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArticleConverter {
    /**
     * 把数据库文章对象转换成列表页返回对象，只放列表需要展示的字段。
     */
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
        vo.setHeatScore(roundHeatScore(heatScore));
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }

    /**
     * 把数据库文章对象转换成详情页返回对象，包含正文、版块名、热度等完整信息。
     */
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
        vo.setHeatScore(roundHeatScore(heatScore));
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }

    private static Double roundHeatScore(Double heatScore) {
        if (heatScore == null) {
            return null;
        }
        return BigDecimal.valueOf(heatScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
