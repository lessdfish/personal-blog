package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.cache.MultiLevelCacheService;
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
    @Autowired(required = false)
    private MultiLevelCacheService multiLevelCacheService;

    /**
     * 获取 permissionCodesByRoleId：返回当前对象里保存的这个值。
     */
    public List<String> getPermissionCodesByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        String key = RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY + roleId;
        if (multiLevelCacheService != null) {
            return List.of(multiLevelCacheService.get(
                    key,
                    String[].class,
                    () -> sanitize(roleMapper.selectPermissionCodesByRoleId(roleId)).toArray(new String[0])
            ));
        }
        List<String> cached = readCache(key);
        if (!cached.isEmpty()) {
            return cached;
        }
        return writeCache(key, roleMapper.selectPermissionCodesByRoleId(roleId));
    }

    /**
     * 获取 permissionCodesByRoleCode：返回当前对象里保存的这个值。
     */
    public List<String> getPermissionCodesByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        String key = RedisKeyConstants.ROLE_PERMISSION_BY_CODE_KEY + roleCode;
        if (multiLevelCacheService != null) {
            return List.of(multiLevelCacheService.get(
                    key,
                    String[].class,
                    () -> sanitize(roleMapper.selectPermissionCodesByRoleCode(roleCode)).toArray(new String[0])
            ));
        }
        List<String> cached = readCache(key);
        if (!cached.isEmpty()) {
            return cached;
        }
        return writeCache(key, roleMapper.selectPermissionCodesByRoleCode(roleCode));
    }

    /**
     * 读取缓存数据：把缓存里的字符串还原成业务需要的对象。
     */
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

    /**
     * 写入缓存数据：把业务数据保存到缓存中，减少后续查询压力。
     */
    private List<String> writeCache(String key, List<String> values) {
        List<String> safeValues = sanitize(values);
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

    /**
     * 清理权限编码列表：去掉空值和重复值，保证缓存内容稳定。
     */
    private List<String> sanitize(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
