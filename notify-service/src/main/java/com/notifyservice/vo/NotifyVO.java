package com.notifyservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName:NotifyVO
 * Package:com.notifyservice.vo
 * Description:通知视图对象
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Data
public class NotifyVO {
    private Long id;
    private Integer type;
    private String title;
    private String content;
    private Long articleId;
    private Long commentId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Integer isRead;
    private LocalDateTime createTime;
}
