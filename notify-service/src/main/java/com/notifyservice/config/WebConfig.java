package com.notifyservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ClassName:WebConfig
 * Package:com.notifyservice.config
 * Description:Web配置
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 注册 Web 拦截器：配置哪些接口需要登录校验，哪些公开接口可以放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/notify/hello", "/error"
                        );
    }
}
