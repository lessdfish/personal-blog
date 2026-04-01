package com.commentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName:CommentPageQueryDTO
 * Package:com.commentservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/4/1 - 00:17
 * @Version: v1.0
 *
 */
@Data
public class CommentPageQueryDTO {
    @NotNull(message = "文章id不能为空")
    private Long articleId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
