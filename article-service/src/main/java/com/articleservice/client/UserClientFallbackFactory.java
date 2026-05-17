package com.articleservice.client;

import com.articleservice.vo.UserSimpleVO;
import com.blogcommon.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    /**
     * 创建降级版 UserClient：当 user-service 调用失败时，返回空用户列表，避免文章接口整体报错。
     */
    @Override
    public UserClient create(Throwable cause) {
        return userIds -> {
            log.warn("user-service批量用户查询降级, userIds={}", userIds, cause);
            return Result.success(Collections.<UserSimpleVO>emptyList());
        };
    }
}
