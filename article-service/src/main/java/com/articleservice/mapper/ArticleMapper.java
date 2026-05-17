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
    /**
     * 新增文章记录：发布帖子时把文章保存到 tb_article 表。
     */
    @Insert("""
            insert into tb_article(title, summary, content, author_id, board_id, tags, status, view_count,
                                   comment_count, like_count, favorite_count, is_top, is_essence, allow_comment,
                                   hot_score, hot_adjust_score, hot_decay_enabled)
            values(#{title}, #{summary}, #{content}, #{authorId}, #{boardId}, #{tags}, #{status}, #{viewCount},
                   #{commentCount}, #{likeCount}, #{favoriteCount}, #{isTop}, #{isEssence}, #{allowComment},
                   #{hotScore}, #{hotAdjustScore}, #{hotDecayEnabled})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    /**
     * 按条件分页查询正常文章：支持版块、作者、精华和排序条件。
     */
    @SelectProvider(type = ArticleSqlProvider.class, method = "buildPageQuery")
    List<Article> selectPageByCondition(ArticlePageQueryDTO dto);

    /**
     * MySQL 关键词兜底查询：当 ES 搜索不可用时，用 LIKE 查询标题、摘要和标签。
     */
    @SelectProvider(type = ArticleSqlProvider.class, method = "buildPageKeywordFallbackQuery")
    List<Article> selectPageByKeywordFallback(ArticlePageQueryDTO dto);

    /**
     * 按 id 查询任意状态的文章：包括已删除或下架的文章。
     */
    @Select("select * from tb_article where id = #{id}")
    Article selectAnyById(Long id);

    /**
     * 按 id 查询正常文章：只返回 status = 1 的文章。
     */
    @Select("select * from tb_article where id = #{id} and status = 1")
    Article selectById(Long id);

    /**
     * 查询文章浏览量：只取 view_count 这一列。
     */
    @Select("select view_count from tb_article where id = #{id}")
    Integer selectViewCountById(Long id);

    /**
     * 统计正常文章总数：用于热榜分页的总条数。
     */
    @Select("select count(*) from tb_article where status = 1")
    Long countActiveArticles();

    /**
     * 批量按 id 查询文章：根据一组文章 id 一次性取回文章列表。
     */
    @Select("""
            <script>
            select id, title, summary, author_id, board_id, tags, status, view_count,
                   comment_count, like_count, favorite_count, is_top, is_essence,
                   allow_comment, hot_score, hot_adjust_score, hot_decay_enabled,
                   last_hot_refresh_time, create_time, update_time
            from tb_article
            where id in
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<Article> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 更新文章正文信息：编辑帖子时修改标题、摘要、内容、版块和标签。
     */
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

    /**
     * 更新文章状态：用于删除、下架或恢复文章。
     */
    @Update("update tb_article set status = #{status} where id = #{articleId}")
    int updateStatus(@Param("articleId") Long articleId, @Param("status") Integer status);

    /**
     * 更新运营管理信息：管理员修改置顶、精华、评论开关和状态。
     */
    @Update("""
            update tb_article
            set is_top = #{isTop},
                is_essence = #{isEssence},
                allow_comment = #{allowComment},
                status = #{status},
                hot_adjust_score = #{hotAdjustScore},
                hot_decay_enabled = #{hotDecayEnabled}
            where id = #{id}
            """)
    int updateManageInfo(Article article);

    /**
     * 刷新热度快照：把计算后的热度和刷新时间回写到文章表。
     */
    @Update("""
            update tb_article
            set hot_score = #{hotScore},
                last_hot_refresh_time = #{refreshTime}
            where id = #{articleId}
            """)
    int updateHeatSnapshot(@Param("articleId") Long articleId,
                           @Param("hotScore") Double hotScore,
                           @Param("refreshTime") java.time.LocalDateTime refreshTime);

    /**
     * 浏览量加一：用户有效查看文章详情时调用。
     */
    @Update("update tb_article set view_count = view_count + 1 where id = #{id} and status = 1")
    int incrementViewCount(Long id);

    /**
     * 把评论数设置为指定值：评论服务同步文章评论数量时调用。
     */
    @Update("""
            update tb_article
            set comment_count = #{commentCount}
            where id = #{articleId} and status = 1
            """)
    int updateCommentCountTo(@Param("articleId") Long articleId, @Param("commentCount") Integer commentCount);

    /**
     * 把点赞数设置为指定值：用于修正点赞数量，最低不会小于 0。
     */
    @Update("""
            update tb_article
            set like_count = greatest(#{likeCount}, 0)
            where id = #{articleId} and status = 1
            """)
    int updateLikeCountTo(@Param("articleId") Long articleId, @Param("likeCount") Integer likeCount);

    /**
     * 按增量修改点赞数：点赞时加 1，取消点赞时减 1，最低不会小于 0。
     */
    @Update("""
            update tb_article
            set like_count = greatest(like_count + #{delta}, 0)
            where id = #{articleId} and status = 1
            """)
    int incrementLikeCount(@Param("articleId") Long articleId, @Param("delta") int delta);

    /**
     * 把收藏数设置为指定值：用于修正收藏数量，最低不会小于 0。
     */
    @Update("""
            update tb_article
            set favorite_count = greatest(#{favoriteCount}, 0)
            where id = #{articleId} and status = 1
            """)
    int updateFavoriteCountTo(@Param("articleId") Long articleId, @Param("favoriteCount") Integer favoriteCount);

    /**
     * 按增量修改收藏数：收藏时加 1，取消收藏时减 1，最低不会小于 0。
     */
    @Update("""
            update tb_article
            set favorite_count = greatest(favorite_count + #{delta}, 0)
            where id = #{articleId} and status = 1
            """)
    int incrementFavoriteCount(@Param("articleId") Long articleId, @Param("delta") int delta);

    /**
     * 查询热榜前几名：按置顶、精华、点赞、评论、浏览等条件排序。
     */
    @Select("""
            select id, title, summary, author_id, board_id, tags, status, view_count,
                   comment_count, like_count, favorite_count, is_top, is_essence,
                   allow_comment, hot_score, hot_adjust_score, hot_decay_enabled,
                   last_hot_refresh_time, create_time, update_time
            from tb_article
            where status = 1
            order by hot_score desc, id desc
            limit #{limit}
            """)
    List<Article> selectHotList(int limit);

    /**
     * 分页查询热榜：用于热榜页面按页加载文章。
     */
    @Select("""
            select id, title, summary, author_id, board_id, tags, status, view_count,
                   comment_count, like_count, favorite_count, is_top, is_essence,
                   allow_comment, hot_score, hot_adjust_score, hot_decay_enabled,
                   last_hot_refresh_time, create_time, update_time
            from tb_article
            where status = 1
            order by hot_score desc, id desc
            limit #{offset}, #{pageSize}
            """)
    List<Article> selectHotPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 分页扫描活跃文章热度字段：定时重算热榜时按批次读取。
     */
    @Select("""
            select id, title, summary, author_id, board_id, tags, status, view_count,
                   comment_count, like_count, favorite_count, is_top, is_essence,
                   allow_comment, hot_score, hot_adjust_score, hot_decay_enabled,
                   last_hot_refresh_time, create_time, update_time
            from tb_article
            where status = 1
            order by id asc
            limit #{offset}, #{pageSize}
            """)
    List<Article> selectActiveForHeatRefresh(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 分批查询正常文章：重建搜索索引时按 id 顺序读取文章数据。
     */
    @Select("""
            select id, title, summary, content, author_id, board_id, tags, status, is_essence
            from tb_article
            where status = 1
            order by id asc
            limit #{offset}, #{pageSize}
            """)
    List<Article> selectActiveForSearchReindex(@Param("offset") int offset, @Param("pageSize") int pageSize);
}
