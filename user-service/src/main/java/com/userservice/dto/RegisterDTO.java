package com.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName:RegisterDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 00:20
 * @Version: v1.0
 *
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4,max = 20,message = "用户名长度必须在4到20位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6,max = 20,message = "密码长度必须在6到20位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20,message = "昵称长度不能超过20位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
    @Pattern(regexp = "^1\\d{10}$",message = "手机号格式不正确")
    private String phone;
}
