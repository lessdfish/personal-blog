package com.userservice.mapper;

import org.apache.ibatis.jdbc.SQL;

import java.util.Map;

/**
 * ClassName:UserSqlProvider
 * Package:com.userservice.mapper
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 00:19
 * @Version: v1.0
 *
 */
public class UserSqlProvider {



        /**
         * 执行 buildSelectUserList 数据库操作：由 MyBatis 根据注解或 SQL 访问数据表。
         */
        public String buildSelectUserList(Map<String, Object> params) {
            String username = (String) params.get("username");
            Integer status = (Integer) params.get("status");

            return new SQL() {{
                SELECT(UserMapper.USER_COLUMNS);
                FROM("tb_user");

                if (username != null && !username.trim().isEmpty()) {
                    WHERE("username like concat('%', #{username}, '%')");
                }

                if (status != null) {
                    WHERE("status = #{status}");
                }

                ORDER_BY("id desc");
            }}.toString();
        }
    }

