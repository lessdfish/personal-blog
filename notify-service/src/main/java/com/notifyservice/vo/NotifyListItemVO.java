package com.notifyservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotifyListItemVO {
    private Long id;
    private Integer type;
    private String title;
    private Long articleId;
    private Long commentId;
    private Long senderId;
    private Integer isRead;
    private LocalDateTime createTime;
}
