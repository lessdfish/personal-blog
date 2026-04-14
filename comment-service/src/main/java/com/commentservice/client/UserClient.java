package com.commentservice.client;

import com.blogcommon.result.Result;
import com.commentservice.config.FeignConfig;
import com.commentservice.vo.UserSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * ClassName:UserClient
 * Package:com.commentservice.client
 * Description:用户服务Feign客户端
 *
 * @Author:lyp
 * @Create:2026/4/1 - 00:18
 * @Version: v1.0
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {
    
    /**
     * 批量获取用户简要信息
     * @param userIds 用户ID列表
     * @return 用户简要信息列表
     */
    @PostMapping("/user/batch/simple")
    Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds);
}
