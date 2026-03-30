package com.articleservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:ArticleDetailVO
 * Package:com.articleservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/30 - 23:52
 * @Version: v1.0
 *
 */
@Data
public class ArticleDetailVO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
