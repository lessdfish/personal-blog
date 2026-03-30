package com.userservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:User
 * Package:com.userservice.entity
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 00:08
 * @Version: v1.0
 *
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long roleId;
}
