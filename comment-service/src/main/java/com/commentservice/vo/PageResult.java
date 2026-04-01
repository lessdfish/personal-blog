package com.commentservice.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName:PageResult
 * Package:com.commentservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/4/1 - 00:18
 * @Version: v1.0
 *
 */
@Data
public class PageResult<T> {
    private Long total;
    private List<T> list;
}
