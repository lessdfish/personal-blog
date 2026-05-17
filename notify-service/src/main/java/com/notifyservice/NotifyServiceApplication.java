package com.notifyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.notifyservice", "com.blogcommon"})
public class NotifyServiceApplication {

    /**
     * 程序启动入口：启动当前模块的 Spring Boot 服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(NotifyServiceApplication.class, args);
    }

}
