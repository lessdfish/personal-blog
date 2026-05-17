package com.articleservice.mapper;

import com.articleservice.vo.ArticlePageQueryDTO;
import org.apache.ibatis.jdbc.SQL;

public class ArticleSqlProvider {
    static final String LIST_COLUMNS = """
            id, title, summary, author_id, board_id, tags, status, view_count,
            comment_count, like_count, favorite_count, is_top, is_essence,
            allow_comment, hot_score, hot_adjust_score, hot_decay_enabled,
            last_hot_refresh_time, create_time, update_time
            """;

    /**
     * 生成普通分页查询 SQL：根据版块、作者、精华和排序条件动态拼接查询语句。
     */
    public String buildPageQuery(ArticlePageQueryDTO dto) {
        SQL sql = new SQL()
                .SELECT(LIST_COLUMNS)
                .FROM("tb_article");

        sql.WHERE("status = 1");
        if (dto.getBoardId() != null) {
            sql.WHERE("board_id = #{boardId}");
        }
        if (dto.getAuthorId() != null) {
            sql.WHERE("author_id = #{authorId}");
        }
        if (Integer.valueOf(1).equals(dto.getOnlyEssence())) {
            sql.WHERE("is_essence = 1");
        }
        String sortBy = dto.getSortBy();
        if ("hot".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("hot_score desc, id desc");
        } else if ("comment".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("is_top desc, comment_count desc, id desc");
        } else {
            sql.ORDER_BY("is_top desc, id desc");
        }
        return sql.toString();
    }

    /**
     * 生成关键词兜底查询 SQL：ES 搜索失败时，用 MySQL LIKE 拼出能搜索关键词的语句。
     */
    public String buildPageKeywordFallbackQuery(ArticlePageQueryDTO dto) {
        SQL sql = new SQL()
                .SELECT(LIST_COLUMNS)
                .FROM("tb_article");

        sql.WHERE("status = 1");
        if (dto.getBoardId() != null) {
            sql.WHERE("board_id = #{boardId}");
        }
        if (dto.getAuthorId() != null) {
            sql.WHERE("author_id = #{authorId}");
        }
        if (Integer.valueOf(1).equals(dto.getOnlyEssence())) {
            sql.WHERE("is_essence = 1");
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            sql.WHERE("(title like concat('%', #{keyword}, '%') or summary like concat('%', #{keyword}, '%') or tags like concat('%', #{keyword}, '%'))");
        }

        String sortBy = dto.getSortBy();
        if ("hot".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("hot_score desc, id desc");
        } else if ("comment".equalsIgnoreCase(sortBy)) {
            sql.ORDER_BY("is_top desc, comment_count desc, id desc");
        } else {
            sql.ORDER_BY("is_top desc, id desc");
        }
        return sql.toString();
    }
}
