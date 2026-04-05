package com.articleservice.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long authorId;
    private Long boardId;
    private String tags;
    private Integer status;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer isTop;
    private Integer isEssence;
    private Integer allowComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
