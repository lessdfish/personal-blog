package com.userservice.entity;

import lombok.Data;

/**
 * ClassName:Role
 * Package:com.userservice.entity
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 20:56
 * @Version: v1.0
 *
 */
@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
}
