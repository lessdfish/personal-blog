package com.commentservice.client;

import com.blogcommon.result.Result;
import com.commentservice.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

    /**
     * 创建数据：接收请求参数，校验后保存一条新记录。
     */
@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return userIds -> {
            log.warn("user-service批量用户查询降级, userIds={}", userIds, cause);
            return Result.success(Collections.<UserSimpleVO>emptyList());
        };
    }
}
