package com.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName:CommentCreateDTO
 * Package:com.commentservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:40
 * @Version: v1.0
 *
 */
@Data
public class CommentCreateDTO {
    @NotNull(message = "需要文章id")
    private Long articleId;

    private Long parentId;

    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;
}
