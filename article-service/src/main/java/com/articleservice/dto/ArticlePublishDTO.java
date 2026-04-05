package com.articleservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticlePublishDTO {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 255, message = "文章标题长度不能超过255")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private Long boardId;

    @Size(max = 255, message = "标签长度不能超过255")
    private String tags;
}
