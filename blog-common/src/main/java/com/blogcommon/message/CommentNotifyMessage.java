package com.blogcommon.message;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ClassName:CommentNotifyMessage
 * Package:com.blogcommon.message
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/30 - 23:55
 * @Version: v1.0
 *
 */
@Data
public class CommentNotifyMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long articleId;
    private Long commentId;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String articleTitle;
    private String content;
    private LocalDateTime createdAt = LocalDateTime.now();
}
