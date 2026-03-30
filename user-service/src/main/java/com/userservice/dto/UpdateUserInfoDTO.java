package com.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName:UpdateUserInfoDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 22:44
 * @Version: v1.0
 *
 */
@Data
public class UpdateUserInfoDTO {
    @Size(max = 20, message = "昵称长度不能超过20位")
    private String nickname;
    @Size(max = 255, message = "头像地址长度不能超过255位")
    private String avatar;
    @Email(message = "邮箱格式不正确")
    private String email;
    @Pattern(regexp = "^1\\d{10}$",message = "手机号格式不正确")
    private String phone;
}
