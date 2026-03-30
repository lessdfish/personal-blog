package com.userservice.config;

import java.lang.annotation.*;

/**
 * ClassName:AdminOnly
 * Package:com.userservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 22:07
 * @Version: v1.0
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminOnly {
}
