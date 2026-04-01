package com.commentservice.mapper;

import com.commentservice.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ClassName:CommentMapper
 * Package:com.commentservice.mapper
 * Description:评论数据访问层
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:42
 * @Version: v1.0
 */
@Mapper
public interface CommentMapper {

    /**
     * 插入评论
     */
    @Insert("""
        insert into tb_comment(article_id, parent_id, user_id, notify_user_id, content, status)
        values(#{articleId}, #{parentId}, #{userId}, #{notifyUserId}, #{content}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /**
     * 根据ID查询评论
     */
    @Select("""
        select * from tb_comment
        where id = #{id} and status = 1
        """)
    Comment selectById(Long id);

    /**
     * 分页查询一级评论（根评论）
     */
    @Select("""
        select * from tb_comment
        where article_id = #{articleId}
          and status = 1
          and parent_id is null
        order by id desc
        limit #{offset}, #{pageSize}
        """)
    List<Comment> selectRootCommentsByArticleId(@Param("articleId") Long articleId,
                                                @Param("offset") Integer offset,
                                                @Param("pageSize") Integer pageSize);

    /**
     * 统计一级评论数量
     */
    @Select("""
        select count(*)
        from tb_comment
        where article_id = #{articleId}
          and status = 1
          and parent_id is null
        """)
    Long countRootCommentsByArticleId(Long articleId);

    /**
     * 查询子评论（根据父评论ID列表）
     */
    @Select("""
        <script>
        select * from tb_comment
        where article_id = #{articleId}
          and status = 1
          and parent_id in 
        <foreach collection='parentIds' item='id' open='(' separator=',' close=')'>
           #{id}
        </foreach>
        order by id asc
        </script>
        """)
    List<Comment> selectChildrenByParentIds(@Param("articleId") Long articleId,
                                            @Param("parentIds") List<Long> parentIds);

    /**
     * 根据文章ID查询所有评论
     */
    @Select("""
        select * from tb_comment
        where article_id = #{articleId}
          and status = 1
        order by id desc
        """)
    List<Comment> selectByArticleId(Long articleId);

    /**
     * 根据ID和用户ID删除评论（只能删除自己的评论）
     */
    @Delete("""
        delete from tb_comment
        where id = #{id} and user_id = #{userId}
        """)
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
