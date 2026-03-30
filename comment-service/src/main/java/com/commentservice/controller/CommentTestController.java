package com.commentservice.controller;

import com.blogcommon.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName:CommentTestController
 * Package:com.commentservice.controller
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 22:11
 * @Version: v1.0
 *
 */
@RestController
public class CommentTestController {
    @GetMapping("/comment/hello")
    public Result<String> hello() {
        return Result.success("comment-service ok");
    }
}
