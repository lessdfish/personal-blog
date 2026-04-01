package com.userservice.vo;

import lombok.Data;

/**
 * ClassName:UserSimpleVO
 * Package:com.userservice.vo
 * Description:用户简要信息VO
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Data
public class UserSimpleVO {
    private Long id;
    private String name;
    private String avatar;
}
