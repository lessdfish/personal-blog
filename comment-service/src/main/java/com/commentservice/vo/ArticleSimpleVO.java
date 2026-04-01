package com.commentservice.vo;

import lombok.Data;

/**
 * ClassName:ArticleSimpleVO
 * Package:com.commentservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/4/1 - 00:18
 * @Version: v1.0
 *
 */
@Data
public class ArticleSimpleVO {
    private Long id;
    private Long authorId;
    private String title;
}
