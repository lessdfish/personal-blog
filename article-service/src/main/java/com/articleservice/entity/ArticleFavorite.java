package com.articleservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleFavorite {
    private Long id;
    private Long articleId;
    private Long userId;
    private LocalDateTime createTime;
}
