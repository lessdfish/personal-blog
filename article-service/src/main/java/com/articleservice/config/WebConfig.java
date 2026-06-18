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
import com.blogcommon.web.CommonJwtInterceptor;
import com.blogcommon.auth.JwtRequestAuthenticator;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtRequestAuthenticator jwtRequestAuthenticator;

    /**
     * 构造 Web 配置：注入 JWT 拦截器，后面才能把它注册到接口路径上。
     */
    public WebConfig(JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

    /**
     * 注册拦截器：大部分文章接口需要登录，公开列表、详情、Swagger 等路径放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CommonJwtInterceptor(jwtRequestAuthenticator))
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
