package com.articleservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName:OpenApiConfig
 * Package:com.articleservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 22:01
 * @Version: v1.0
 *
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI articleOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("Article Service API")
                        .description("博客文章模块接口文档")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Authorization")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));

    }
}
