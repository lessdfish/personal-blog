package com.articleservice.mapper;

import com.articleservice.vo.ArticlePageQueryDTO;
import org.apache.ibatis.jdbc.SQL;

public class ArticleSqlProvider {
    public String buildPageQuery(ArticlePageQueryDTO dto) {
        SQL sql = new SQL()
                .SELECT("*")
                .FROM("tb_article");

        sql.WHERE("status = 1");
        if (dto.getBoardId() != null) {
            sql.WHERE("board_id = #{boardId}");
        }
        if (dto.getAuthorId() != null) {
            sql.WHERE("author_id = #{authorId}");
        }
        if (dto.getOnlyEssence() != null && dto.getOnlyEssence() == 1) {
            sql.WHERE("is_essence = 1");
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            sql.WHERE("(title like concat('%', #{keyword}, '%') or summary like concat('%', #{keyword}, '%') or tags like concat('%', #{keyword}, '%'))");
        }

        String sortBy = dto.getSortBy();
        if ("hot".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("is_top desc, like_count desc, comment_count desc, view_count desc, id desc");
        } else if ("comment".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("is_top desc, comment_count desc, id desc");
        } else {
            sql.ORDER_BY("is_top desc, id desc");
        }
        return sql.toString();
    }
}
