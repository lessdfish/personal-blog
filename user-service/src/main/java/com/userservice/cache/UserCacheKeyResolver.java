package com.userservice.cache;

import com.blogcommon.cache.CacheInvalidationMessage;
import com.blogcommon.cache.CacheKeyResolver;
import com.blogcommon.constant.RedisKeyConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户缓存键解析器：把用户、角色和权限表变更转换为用户权限缓存失效键。
 */
@Component
public class UserCacheKeyResolver implements CacheKeyResolver {
    @Override
    public boolean supports(String tableName) {
        return List.of("tb_user", "tb_role", "tb_permission", "tb_role_permission").contains(tableName);
    }

    @Override
    public Collection<String> resolveKeys(CacheInvalidationMessage message) {
        List<String> keys = new ArrayList<>();
        String roleId = message.getColumns() == null ? null : message.getColumns().get("role_id");
        if (roleId != null && !roleId.isBlank()) {
            keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY + roleId);
        }
        keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY);
        keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_CODE_KEY);
        return keys;
    }
}
