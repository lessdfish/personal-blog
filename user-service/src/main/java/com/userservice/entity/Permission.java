package com.userservice.entity;

import lombok.Data;

@Data
public class Permission {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String description;
}
