package com.commentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.commentservice", "com.blogcommon"})
@EnableDiscoveryClient
@EnableFeignClients
public class CommentServiceApplication {

    /**
     * 程序启动入口：启动当前模块的 Spring Boot 服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }

}
