package com.userservice.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName:UserInfoVO
 * Package:com.userservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 18:55
 * @Version: v1.0
 *
 */
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private String roleCode;
    private String roleName;
    private List<String> permissionCodes;
}
