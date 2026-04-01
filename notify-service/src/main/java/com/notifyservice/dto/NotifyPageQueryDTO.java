package com.notifyservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName:NotifyPageQueryDTO
 * Package:com.notifyservice.dto
 * Description:通知分页查询DTO
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Data
public class NotifyPageQueryDTO {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于0")
    private Integer pageSize;
}
