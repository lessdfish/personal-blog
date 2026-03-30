package com.userservice.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName:PageVO
 * Package:com.userservice.vo
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 00:01
 * @Version: v1.0
 *
 */
@Data
public class PageVO<T>{
    private Long total;
    private List<T> list;
}
