package com.commentservice.converter;

import com.commentservice.dto.CommentCreateDTO;
import com.commentservice.entity.Comment;
import com.commentservice.vo.CommentVO;

/**
 * ClassName:CommentConverter
 * Package:com.commentservice.converter
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:58
 * @Version: v1.0
 *
 */

public class CommentConverter {
    public static Comment toEntity(Long userId, CommentCreateDTO dto) {
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(dto.getParentId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setStatus(1);
        return comment;
    }

    public static CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setParentId(comment.getParentId());
        vo.setUserId(comment.getUserId());
        vo.setNotifyUserId(comment.getNotifyUserId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
