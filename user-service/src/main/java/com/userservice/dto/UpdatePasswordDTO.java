package com.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName:UpdatePasswordDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 23:02
 * @Version: v1.0
 *
 */
@Data
public class UpdatePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6,max = 20,message = "新密码长度必须在6到20位之间")
    private String newPassword;
}
