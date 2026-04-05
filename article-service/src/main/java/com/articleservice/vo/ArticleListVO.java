package com.articleservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleListVO {
    private Long id;
    private String title;
    private String summary;
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
    private Double heatScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
