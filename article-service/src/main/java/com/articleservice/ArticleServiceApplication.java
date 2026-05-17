package com.articleservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@MapperScan(value = "com.articleservice.mapper")
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@EnableFeignClients(basePackages = "com.articleservice.client")
@SpringBootApplication(scanBasePackages = {"com.articleservice", "com.blogcommon"})
public class ArticleServiceApplication {

    /**
     * 程序启动入口：启动 Spring Boot 的 article-service 服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(ArticleServiceApplication.class, args);
    }

}
