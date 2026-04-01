package com.commentservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName:CommentVO
 * Package:com.commentservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:43
 * @Version: v1.0
 *
 */
@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long userId;
    private Long notifyUserId;
    private String content;
    private LocalDateTime createTime;

    private String userName;
    private String userAvatar;
    private String notifyUserName;
    private List<CommentVO> children;
}
