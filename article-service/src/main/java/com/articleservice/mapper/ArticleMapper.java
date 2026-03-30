package com.articleservice.mapper;

import com.articleservice.entity.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lyp
 * @since 2026-03-28
 */
@Mapper
public interface ArticleMapper{
    @Insert("""
            insert into tb_article(title, content, author_id, status, view_count)
            values(#{title}, #{content}, #{authorId}, #{status}, #{viewCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Select("""
            select * from tb_article
            where status = 1
            order by id desc
            """)
    List<Article> selectPublishedList();
}
