package com.userservice.vo;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionVO {
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissionCodes;
}
