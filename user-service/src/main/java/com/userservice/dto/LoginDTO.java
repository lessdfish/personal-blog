package com.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ClassName:LoginDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 17:55
 * @Version: v1.0
 *
 */
@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
