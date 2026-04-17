package com.userservice.vo;

import lombok.Data;

@Data
public class CurrentUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private SessionInfoVO sessionInfo;
}
