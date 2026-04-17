package com.articleservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
@MapperScan(value = "com.articleservice.mapper")
@EnableDiscoveryClient
@EnableAsync
@EnableFeignClients(basePackages = "com.articleservice.client")
@SpringBootApplication(scanBasePackages = {"com.articleservice", "com.blogcommon"})
public class ArticleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleServiceApplication.class, args);
    }

}
