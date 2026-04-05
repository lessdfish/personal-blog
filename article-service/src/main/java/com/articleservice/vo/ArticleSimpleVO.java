package com.articleservice.vo;

import lombok.Data;

@Data
public class ArticleSimpleVO {
    private Long id;
    private Long authorId;
    private String title;
    private Integer allowComment;
}
