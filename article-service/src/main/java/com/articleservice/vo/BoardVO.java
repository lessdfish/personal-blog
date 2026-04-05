package com.articleservice.vo;

import lombok.Data;

@Data
public class BoardVO {
    private Long id;
    private String boardName;
    private String boardCode;
    private String description;
    private Integer sortOrder;
}
