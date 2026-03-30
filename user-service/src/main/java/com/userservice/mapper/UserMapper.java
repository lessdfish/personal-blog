package com.userservice.mapper;

import com.userservice.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ClassName:UserMapper
 * Package:com.userservice.mapper
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 00:08
 * @Version: v1.0
 *
 */
@Mapper
public interface UserMapper {
    @Select("select * from tb_user where id=#{id}")
    User selectById(Long id);

    @Select("select * from tb_user where username = #{username}")
    User selectByUsername(String username);

    @Insert("""
            insert into tb_user(username, password, nickname, email, phone, status)
            values(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
        update tb_user
        set nickname = #{nickname},
            avatar = #{avatar},
            email = #{email},
            phone = #{phone}
        where id = #{id}
        """)
    int updateUserInfo(User user);

    @Update("""
        update tb_user
        set password = #{password}
        where id = #{id}
        """)
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("""
        select * from tb_user
        order by id desc
        """)
    List<User> selectUserList();

    @SelectProvider(type = UserSqlProvider.class, method = "buildSelectUserList")
    List<User> selectUserListByCondition(@Param("username") String username, @Param("status") Integer status);

    @Update("""
        update tb_user
        set status = #{status}
        where id = #{userId}
        """)
    int updateUserStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
