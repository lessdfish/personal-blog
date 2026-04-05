package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.userservice.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RolePermissionCacheService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public List<String> getPermissionCodesByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        String key = RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY + roleId;
        List<String> cached = readCache(key);
        if (!cached.isEmpty()) {
            return cached;
        }
        return writeCache(key, roleMapper.selectPermissionCodesByRoleId(roleId));
    }

    public List<String> getPermissionCodesByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        String key = RedisKeyConstants.ROLE_PERMISSION_BY_CODE_KEY + roleCode;
        List<String> cached = readCache(key);
        if (!cached.isEmpty()) {
            return cached;
        }
        return writeCache(key, roleMapper.selectPermissionCodesByRoleCode(roleCode));
    }

    private List<String> readCache(String key) {
        if (stringRedisTemplate == null) {
            return List.of();
        }
        String value = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private List<String> writeCache(String key, List<String> values) {
        List<String> safeValues = values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    String.join(",", safeValues),
                    RedisKeyConstants.ROLE_PERMISSION_CACHE_EXPIRE,
                    TimeUnit.SECONDS
            );
        }
        return safeValues;
    }
}
