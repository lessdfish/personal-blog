package com.articleservice.client;

import com.articleservice.config.FeignConfig;
import com.articleservice.vo.UserSimpleVO;
import com.blogcommon.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service", configuration = FeignConfig.class, fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    /**
     * 调用 user-service：根据一批用户 id 查询用户的简要信息，比如昵称。
     */
    @PostMapping("/user/batch/simple")
    Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds);
}
