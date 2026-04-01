package com.notifyservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:Notify
 * Package:com.notifyservice.entity
 * Description:通知实体
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Data
public class Notify {
    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private Long articleId;
    private Long commentId;
    private Long senderId;
    private Integer isRead;
    private LocalDateTime createTime;
}
