package com.userservice.vo;

import lombok.Data;

/**
 * ClassName:LoginVO
 * Package:com.userservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 19:08
 * @Version: v1.0
 *
 */
@Data
public class LoginVO {
    private String token;
    private UserInfoVO userInfoVO;
}
