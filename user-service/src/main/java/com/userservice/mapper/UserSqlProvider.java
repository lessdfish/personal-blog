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



        public String buildSelectUserList(Map<String, Object> params) {
            String username = (String) params.get("username");
            Integer status = (Integer) params.get("status");

            return new SQL() {{
                SELECT("*");
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

