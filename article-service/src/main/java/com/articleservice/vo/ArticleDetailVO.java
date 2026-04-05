package com.articleservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDetailVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long authorId;
    private Long boardId;
    private String boardName;
    private String tags;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer isTop;
    private Integer isEssence;
    private Integer allowComment;
    private Double heatScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
