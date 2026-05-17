package com.articleservice.mapper;

import com.articleservice.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper {
    /**
     * 新增点赞记录：insert ignore 表示重复点赞不会重复插入。
     */
    @Insert("""
            insert ignore into tb_article_like(article_id, user_id)
            values(#{articleId}, #{userId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(ArticleLike articleLike);

    /**
     * 删除点赞记录：用户取消点赞时调用。
     */
    @Delete("""
            delete from tb_article_like
            where article_id = #{articleId} and user_id = #{userId}
            """)
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 查询某用户是否点赞某文章：返回匹配记录数量。
     */
    @Select("""
            select count(*)
            from tb_article_like
            where article_id = #{articleId} and user_id = #{userId}
            """)
    Long countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 统计某篇文章的点赞总数。
     */
    @Select("""
            select count(*)
            from tb_article_like
            where article_id = #{articleId}
            """)
    Long countByArticle(@Param("articleId") Long articleId);
}
