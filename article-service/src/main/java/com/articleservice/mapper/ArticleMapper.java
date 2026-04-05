package com.articleservice.mapper;

import com.articleservice.entity.Article;
import com.articleservice.vo.ArticlePageQueryDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper {
    @Insert("""
            insert into tb_article(title, summary, content, author_id, board_id, tags, status, view_count,
                                   comment_count, like_count, favorite_count, is_top, is_essence, allow_comment)
            values(#{title}, #{summary}, #{content}, #{authorId}, #{boardId}, #{tags}, #{status}, #{viewCount},
                   #{commentCount}, #{likeCount}, #{favoriteCount}, #{isTop}, #{isEssence}, #{allowComment})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @SelectProvider(type = ArticleSqlProvider.class, method = "buildPageQuery")
    List<Article> selectPageByCondition(ArticlePageQueryDTO dto);

    @Select("select * from tb_article where id = #{id}")
    Article selectAnyById(Long id);

    @Select("select * from tb_article where id = #{id} and status = 1")
    Article selectById(Long id);

    @Select("select count(*) from tb_article where status = 1")
    Long countActiveArticles();

    @Select("""
            <script>
            select * from tb_article
            where id in
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<Article> selectByIds(@Param("ids") List<Long> ids);

    @Update("""
            update tb_article
            set title = #{title},
                summary = #{summary},
                content = #{content},
                board_id = #{boardId},
                tags = #{tags}
            where id = #{id}
            """)
    int updateArticle(Article article);

    @Update("update tb_article set status = #{status} where id = #{articleId}")
    int updateStatus(@Param("articleId") Long articleId, @Param("status") Integer status);

    @Update("""
            update tb_article
            set is_top = #{isTop},
                is_essence = #{isEssence},
                allow_comment = #{allowComment},
                status = #{status}
            where id = #{id}
            """)
    int updateManageInfo(Article article);

    @Update("update tb_article set view_count = view_count + 1 where id = #{id} and status = 1")
    int incrementViewCount(Long id);

    @Update("""
            update tb_article
            set comment_count = #{commentCount}
            where id = #{articleId} and status = 1
            """)
    int updateCommentCountTo(@Param("articleId") Long articleId, @Param("commentCount") Integer commentCount);

    @Update("""
            update tb_article
            set like_count = greatest(#{likeCount}, 0)
            where id = #{articleId} and status = 1
            """)
    int updateLikeCountTo(@Param("articleId") Long articleId, @Param("likeCount") Integer likeCount);

    @Update("""
            update tb_article
            set favorite_count = greatest(#{favoriteCount}, 0)
            where id = #{articleId} and status = 1
            """)
    int updateFavoriteCountTo(@Param("articleId") Long articleId, @Param("favoriteCount") Integer favoriteCount);

    @Select("""
            select * from tb_article
            where status = 1
            order by is_top desc, is_essence desc, like_count desc, comment_count desc, view_count desc, id desc
            limit #{limit}
            """)
    List<Article> selectHotList(int limit);

    @Select("""
            select * from tb_article
            where status = 1
            order by is_top desc, is_essence desc, like_count desc, comment_count desc, view_count desc, id desc
            limit #{offset}, #{pageSize}
            """)
    List<Article> selectHotPage(@Param("offset") int offset, @Param("pageSize") int pageSize);
}
