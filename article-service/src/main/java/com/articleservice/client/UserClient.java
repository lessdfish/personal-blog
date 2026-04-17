package com.articleservice.client;

import com.articleservice.config.FeignConfig;
import com.articleservice.vo.UserSimpleVO;
import com.blogcommon.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @PostMapping("/user/batch/simple")
    Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds);
}
