package com.articleservice.mapper;

import com.articleservice.entity.ArticleFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleFavoriteMapper {
    @Insert("""
            insert ignore into tb_article_favorite(article_id, user_id)
            values(#{articleId}, #{userId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(ArticleFavorite favorite);

    @Delete("""
            delete from tb_article_favorite
            where article_id = #{articleId} and user_id = #{userId}
            """)
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("""
            select count(*)
            from tb_article_favorite
            where article_id = #{articleId} and user_id = #{userId}
            """)
    Long countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("""
            select article_id
            from tb_article_favorite
            where user_id = #{userId}
            order by id desc
            limit #{offset}, #{pageSize}
            """)
    List<Long> selectArticleIdsByUser(@Param("userId") Long userId,
                                      @Param("offset") Integer offset,
                                      @Param("pageSize") Integer pageSize);

    @Select("select count(*) from tb_article_favorite where user_id = #{userId}")
    Long countByUser(Long userId);

    @Select("select count(*) from tb_article_favorite where article_id = #{articleId}")
    Long countByArticle(@Param("articleId") Long articleId);
}
