package com.articleservice.dto;

import lombok.Data;

@Data
public class ArticleManageDTO {
    private Integer isTop;
    private Integer isEssence;
    private Integer allowComment;
    private Integer status;
    private Double hotAdjustScore;
    private Integer hotDecayEnabled;
}
