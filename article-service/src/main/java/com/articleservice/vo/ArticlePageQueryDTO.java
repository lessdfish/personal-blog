package com.articleservice.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName:ArticlePageQueryDTO
 * Package:com.articleservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/29 - 00:06
 * @Version: v1.0
 *
 */
    @Data
    public class ArticlePageQueryDTO {

        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码必须大于0")
        private Integer pageNum;

        @NotNull(message = "每页条数不能为空")
        @Min(value = 1, message = "每页条数必须大于0")
        private Integer pageSize;
    }

