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

    @Select("select * from tb_user where nickname = #{nickname} limit 1")
    User selectByNickname(String nickname);

    @Select("select * from tb_user where email = #{email} limit 1")
    User selectByEmail(String email);

    @Select("select * from tb_user where phone = #{phone} limit 1")
    User selectByPhone(String phone);

    @Select("select * from tb_user where nickname = #{nickname} and id <> #{id} limit 1")
    User selectByNicknameExcludeId(@Param("nickname") String nickname, @Param("id") Long id);

    @Select("select * from tb_user where email = #{email} and id <> #{id} limit 1")
    User selectByEmailExcludeId(@Param("email") String email, @Param("id") Long id);

    @Select("select * from tb_user where phone = #{phone} and id <> #{id} limit 1")
    User selectByPhoneExcludeId(@Param("phone") String phone, @Param("id") Long id);

    @Insert("""
            insert into tb_user(username, password, nickname, email, phone, status, role_id)
            values(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{status}, #{roleId})
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

    @Update("""
        update tb_user
        set role_id = #{roleId}
        where id = #{userId}
        """)
    int updateUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("""
        <script>
        select * from tb_user
        where id in
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    List<User> selectByIds(@Param("ids") List<Long> ids);
}
