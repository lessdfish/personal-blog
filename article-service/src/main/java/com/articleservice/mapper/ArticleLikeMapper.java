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
    @Insert("""
            insert ignore into tb_article_like(article_id, user_id)
            values(#{articleId}, #{userId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(ArticleLike articleLike);

    @Delete("""
            delete from tb_article_like
            where article_id = #{articleId} and user_id = #{userId}
            """)
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("""
            select count(*)
            from tb_article_like
            where article_id = #{articleId} and user_id = #{userId}
            """)
    Long countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("""
            select count(*)
            from tb_article_like
            where article_id = #{articleId}
            """)
    Long countByArticle(@Param("articleId") Long articleId);
}
