package com.commentservice.config;

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


    public WebConfig(JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

    /**
     * 注册 Web 拦截器：配置哪些接口需要登录校验，哪些公开接口可以放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CommonJwtInterceptor(jwtRequestAuthenticator))
                .addPathPatterns("/comment/**")
                .excludePathPatterns("/comment/article/**",
                        "/comment/page");
    }
}
