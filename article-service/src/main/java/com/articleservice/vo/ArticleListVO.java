package com.articleservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:ArticleListVO
 * Package:com.articleservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/29 - 00:00
 * @Version: v1.0
 *
 */
@Data
public class ArticleListVO {
        private Long id;
        private String title;
        private Long authorId;
        private Integer viewCount;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

