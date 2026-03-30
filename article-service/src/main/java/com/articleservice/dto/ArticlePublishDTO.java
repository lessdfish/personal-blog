package com.articleservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName:ArticlePublishDTO
 * Package:com.articleservice.dto
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 22:55
 * @Version: v1.0
 *
 */
@Data
public class ArticlePublishDTO {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 255, message = "文章标题长度不能超过255")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;
}
