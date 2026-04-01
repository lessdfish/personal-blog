package com.commentservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:Comment
 * Package:com.commentservice.entity
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:41
 * @Version: v1.0
 *
 */
@Data
public class Comment {
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long userId;
    private Long notifyUserId;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
