package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class UserActivityService {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public void recordActivity(Long userId) {
        if (userId == null || stringRedisTemplate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        int week = today.get(WeekFields.of(Locale.CHINA).weekOfWeekBasedYear());
        String dayKey = RedisKeyConstants.USER_ACTIVE_DAY_KEY + today.format(DAY_FORMATTER);
        String weekKey = RedisKeyConstants.USER_ACTIVE_WEEK_KEY + today.getYear() + String.format("%02d", week);

        stringRedisTemplate.opsForSet().add(dayKey, userId.toString());
        stringRedisTemplate.opsForSet().add(weekKey, userId.toString());
        stringRedisTemplate.expire(dayKey, 8, TimeUnit.DAYS);
        stringRedisTemplate.expire(weekKey, 30, TimeUnit.DAYS);

        stringRedisTemplate.opsForZSet().incrementScore(RedisKeyConstants.USER_ACTIVE_RANK_KEY, userId.toString(), 1D);
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.USER_LAST_ACTIVE_KEY + userId,
                LocalDateTime.now().format(TIME_FORMATTER),
                30,
                TimeUnit.DAYS
        );
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.USER_ONLINE_KEY + userId,
                "1",
                RedisKeyConstants.USER_ONLINE_EXPIRE,
                TimeUnit.SECONDS
        );
    }
}
