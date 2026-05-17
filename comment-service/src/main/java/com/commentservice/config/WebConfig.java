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
import com.commentservice.config.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                .addPathPatterns("/comment/**")
                .excludePathPatterns("/comment/article/**",
                        "/comment/page");
    }
}
