package com.userservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginVO {
    private String token;
    private LoginUserVO user;
}
