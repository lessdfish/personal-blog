package com.notifyservice.controller;

import com.blogcommon.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName:NotifyTestController
 * Package:com.notifyservice.controller
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 22:12
 * @Version: v1.0
 *
 */
@RestController
public class NotifyTestController {

    @GetMapping("/notify/hello")
    public Result<String> hello() {
        return Result.success("notify-service ok");
    }
}
