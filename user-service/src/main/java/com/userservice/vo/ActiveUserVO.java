package com.userservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActiveUserVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Double activityScore;
    private LocalDateTime lastActiveTime;
}
