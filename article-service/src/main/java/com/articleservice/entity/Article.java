package com.articleservice.entity;


import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;


/**
 * <p>
 * 
 * </p>
 *
 * @author lyp
 * @since 2026-03-28
 */
@Data

public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */

    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 状态：1正常 0删除
     */
    private Integer status;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
