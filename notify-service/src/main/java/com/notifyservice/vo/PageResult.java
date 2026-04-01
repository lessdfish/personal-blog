package com.notifyservice.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName:PageResult
 * Package:com.notifyservice.vo
 * Description:分页结果
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Data
public class PageResult<T> {
    private Long total;
    private List<T> list;
}
