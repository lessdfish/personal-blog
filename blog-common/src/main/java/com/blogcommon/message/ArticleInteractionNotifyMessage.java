package com.blogcommon.message;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ArticleInteractionNotifyMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long articleId;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String articleTitle;
    private String action;
    private LocalDateTime createdAt = LocalDateTime.now();
}
