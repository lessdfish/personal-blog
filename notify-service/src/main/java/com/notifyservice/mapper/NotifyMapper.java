package com.notifyservice.mapper;

import com.notifyservice.entity.Notify;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ClassName:NotifyMapper
 * Package:com.notifyservice.mapper
 * Description:通知数据访问层
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Mapper
public interface NotifyMapper {

    @Insert("""
        insert into tb_notify(user_id, type, title, content, article_id, comment_id, sender_id, is_read)
        values(#{userId}, #{type}, #{title}, #{content}, #{articleId}, #{commentId}, #{senderId}, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notify notify);

    @Select("""
        select * from tb_notify
        where user_id = #{userId}
        order by create_time desc
        limit #{offset}, #{pageSize}
        """)
    List<Notify> selectByUserId(@Param("userId") Long userId,
                                @Param("offset") Integer offset,
                                @Param("pageSize") Integer pageSize);

    @Select("select count(*) from tb_notify where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("select count(*) from tb_notify where user_id = #{userId} and is_read = 0")
    Long countUnreadByUserId(Long userId);

    @Select("select * from tb_notify where id = #{id}")
    Notify selectById(Long id);

    @Update("update tb_notify set is_read = 1 where id = #{id} and user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update tb_notify set is_read = 1 where user_id = #{userId}")
    int markAllAsRead(Long userId);

    @Delete("delete from tb_notify where id = #{id} and user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update tb_notify set is_read = 1 where id in (${ids}) and user_id = #{userId}")
    int batchMarkAsRead(@Param("ids") String ids, @Param("userId") Long userId);
}
