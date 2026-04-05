package com.articleservice.config;

/**
 * ClassName:WebConfig
 * Package:com.articleservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 22:55
 * @Version: v1.0
 *
 */
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/article/page",
                        "/article/page/normal",
                        "/article/page/hot",
                        "/article/detail/**",
                        "/article/hot",
                        "/article/heat/**",
                        "/article/likes/**",
                        "/article/favorites/count/**",
                        "/article/views/**",
                        "/article/simple/**",
                        "/article/board/list",
                        "/article/comment/count/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }
}
