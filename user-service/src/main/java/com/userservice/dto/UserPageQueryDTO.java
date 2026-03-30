package com.userservice.dto;

import lombok.Data;

/**
 * ClassName:UserPageQueryDTO
 * Package:com.userservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 00:17
 * @Version: v1.0
 *
 */
@Data
public class UserPageQueryDTO {
    private Integer pageNum;
    private Integer pageSize;
    private String username;
    private Integer status;
}
