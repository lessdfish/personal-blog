package com.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName:UpdateStatusDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 20:25
 * @Version: v1.0
 *
 */
@Data
public class UpdateUserStatusDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "用户状态不能为空")
    private Integer status;
}
