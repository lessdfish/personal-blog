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
    /**
     * 新增收藏记录：insert ignore 表示重复收藏不会重复插入。
     */
    @Insert("""
            insert ignore into tb_article_favorite(article_id, user_id)
            values(#{articleId}, #{userId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(ArticleFavorite favorite);

    /**
     * 删除收藏记录：用户取消收藏时调用。
     */
    @Delete("""
            delete from tb_article_favorite
            where article_id = #{articleId} and user_id = #{userId}
            """)
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 查询某用户是否收藏某文章：返回匹配记录数量。
     */
    @Select("""
            select count(*)
            from tb_article_favorite
            where article_id = #{articleId} and user_id = #{userId}
            """)
    Long countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 分页查询用户收藏的文章 id：用于“我的收藏”页面。
     */
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

    /**
     * 统计某用户收藏了多少篇文章。
     */
    @Select("select count(*) from tb_article_favorite where user_id = #{userId}")
    Long countByUser(Long userId);

    /**
     * 统计某篇文章被收藏了多少次。
     */
    @Select("select count(*) from tb_article_favorite where article_id = #{articleId}")
    Long countByArticle(@Param("articleId") Long articleId);
}
