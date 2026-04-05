package com.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleDTO {
    @NotNull(message = "userId不能为空")
    private Long userId;

    @NotNull(message = "roleId不能为空")
    private Long roleId;
}
