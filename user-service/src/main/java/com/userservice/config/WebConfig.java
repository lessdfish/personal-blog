package com.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ClassName:WebConfig
 * Package:com.userservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 18:49
 * @Version: v1.0
 *
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    /**
     * 构造 WebConfig：注入这个类运行时需要的依赖。
     */
    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    /**
     * 注册 Web 拦截器：配置哪些接口需要登录校验，哪些公开接口可以放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login",
                        "/user/register",
                        "/user/check",
                        "/user/hello",
                        "/user/parse",
                        "/user/token/refresh",
                        "/user/batch/simple",  // 允许Feign调用
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }
}
