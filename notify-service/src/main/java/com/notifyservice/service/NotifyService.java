package com.notifyservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.cache.MultiLevelCacheService;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.logging.DbWriteAuditLogger;
import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.CommentNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.notifyservice.config.UserContext;
import com.notifyservice.dto.NotifyPageQueryDTO;
import com.notifyservice.entity.Notify;
import com.notifyservice.mapper.NotifyMapper;
import com.notifyservice.vo.NotifyListItemVO;
import com.notifyservice.vo.NotifyVO;
import com.notifyservice.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:NotifyService
 * Package:com.notifyservice.service
 * Description:通知服务
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Service
public class NotifyService {

    @Autowired
    private NotifyMapper notifyMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired(required = false)
    private MultiLevelCacheService multiLevelCacheService;

    private static final int NOTIFY_TYPE_COMMENT = 1;
    private static final int NOTIFY_TYPE_LIKE = 2;
    private static final int NOTIFY_TYPE_FAVORITE = 3;

    /**
     * 处理评论通知消息：把评论事件转换成站内通知。
     */
    /**
     * 处理评论通知消息
     */
    public void handleCommentNotify(CommentNotifyMessage message) {
        Notify notify = new Notify();
        notify.setUserId(message.getReceiverId());
        notify.setType(NOTIFY_TYPE_COMMENT);
        String senderName = defaultSenderName(message.getSenderName());
        notify.setTitle("用户" + senderName + "，评论了你的文章");
        notify.setContent(buildInteractionContent(senderName, "评论", message.getArticleTitle(), message.getContent()));
        notify.setArticleId(message.getArticleId());
        notify.setCommentId(message.getCommentId());
        notify.setSenderId(message.getSenderId());

        notifyMapper.insert(notify);
        DbWriteAuditLogger.logInsert("tb_notify", notify);

        // 增加未读计数缓存
        incrementUnreadCache(message.getReceiverId());
    }

    /**
     * 处理文章互动通知消息：把点赞或收藏事件转换成站内通知。
     */
    public void handleArticleInteractionNotify(ArticleInteractionNotifyMessage message) {
        Notify notify = new Notify();
        notify.setUserId(message.getReceiverId());
        notify.setType(resolveInteractionType(message.getAction()));
        String senderName = defaultSenderName(message.getSenderName());
        String actionText = resolveInteractionActionText(message.getAction());
        notify.setTitle("用户" + senderName + "，" + actionText + "了你的文章");
        notify.setContent(buildInteractionContent(senderName, actionText, message.getArticleTitle(), null));
        notify.setArticleId(message.getArticleId());
        notify.setSenderId(message.getSenderId());

        notifyMapper.insert(notify);
        DbWriteAuditLogger.logInsert("tb_notify", notify);
        incrementUnreadCache(message.getReceiverId());
    }

    /**
     * 增加未读计数缓存
     */
    private void incrementUnreadCache(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, RedisKeyConstants.NOTIFY_UNREAD_EXPIRE, TimeUnit.SECONDS);
            if (multiLevelCacheService != null) {
                multiLevelCacheService.evict(key);
            }
        }
    }

    /**
     * 减少未读数缓存：通知被读或删除后扣减未读数。
     */
    /**
     * 减少未读计数缓存
     */
    private void decrementUnreadCache(Long userId, long count) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            Long current = getUnreadCountFromCache(key);
            long next = Math.max(0L, current - count);
            stringRedisTemplate.opsForValue().set(
                    key,
                    String.valueOf(next),
                    RedisKeyConstants.NOTIFY_UNREAD_EXPIRE,
                    TimeUnit.SECONDS);
            if (multiLevelCacheService != null) {
                multiLevelCacheService.put(key, next);
            }
        }
    }

    /**
     * 删除未读数缓存：缓存不可靠时清掉，下次重新统计。
     */
    /**
     * 删除未读计数缓存（标记全部已读时使用）
     */
    private void deleteUnreadCache(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            stringRedisTemplate.delete(key);
            if (multiLevelCacheService != null) {
                multiLevelCacheService.evict(key);
            }
        }
    }

    /**
     * 分页查询用户通知：返回当前用户的通知列表。
     */
    /**
     * 分页查询当前用户的通知列表
     */
    public PageResult<NotifyListItemVO> pageByUser(Long userId, NotifyPageQueryDTO dto) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long total = notifyMapper.countByUserId(userId);
        if (total == null || Long.valueOf(0L).equals(total)) {
            PageResult<NotifyListItemVO> empty = new PageResult<>();
            empty.setTotal(0L);
            empty.setList(Collections.emptyList());
            return empty;
        }

        List<NotifyListItemVO> voList = notifyMapper.selectSummaryByUserId(userId, offset, pageSize);

        PageResult<NotifyListItemVO> result = new PageResult<>();
        result.setTotal(total);
        result.setList(voList);
        return result;
    }

    /**
     * 查询详情：校验归属后返回指定记录的完整信息。
     */
    public NotifyVO getDetail(Long userId, Long notifyId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Notify notify = notifyMapper.selectByIdAndUserId(notifyId, userId);
        if (notify == null) {
            throw new BusinessException(ResultCode.NOTIFY_NOT_FOUND);
        }
        return toVO(notify);
    }

    /**
     * 查询未读通知数：优先读缓存，缓存没有再查数据库。
     */
    /**
     * 获取未读通知数量（带缓存）
     */
    public Long getUnreadCount(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
        if (multiLevelCacheService != null) {
            Long count = multiLevelCacheService.get(
                    key,
                    Long.class,
                    () -> {
                        Long databaseCount = notifyMapper.countUnreadByUserId(userId);
                        return databaseCount == null ? 0L : databaseCount;
                    }
            );
            return count == null ? 0L : count;
        }
        
        // 先查Redis缓存
        if (stringRedisTemplate != null) {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
        }
        
        // 缓存不存在，查数据库
        Long count = notifyMapper.countUnreadByUserId(userId);
        
        // 写入缓存
        if (stringRedisTemplate != null && count != null) {
            stringRedisTemplate.opsForValue().set(key, count.toString(), 
                    RedisKeyConstants.NOTIFY_UNREAD_EXPIRE, TimeUnit.SECONDS);
        }
        
        return count != null ? count : 0L;
    }

    /**
     * 标记单条通知已读：更新数据库并同步未读数缓存。
     */
    /**
     * 标记单条通知为已读
     */
    public void markAsRead(Long userId, Long notifyId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Notify notify = notifyMapper.selectById(notifyId);
        if (notify == null) {
            throw new BusinessException(ResultCode.NOTIFY_NOT_FOUND);
        }

        if (!notify.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (Integer.valueOf(1).equals(notify.getIsRead())) {
            syncUnreadCache(userId);
            return;
        }

        if (notifyMapper.markAsRead(notifyId, userId) <= 0) {
            throw new BusinessException(ResultCode.NOTIFY_READ_FAILED);
        }

        syncUnreadCache(userId);
    }

    /**
     * 标记全部通知已读：把当前用户所有未读通知改为已读。
     */
    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        notifyMapper.markAllAsRead(userId);
        deleteUnreadCache(userId);
    }

    /**
     * 删除数据：校验权限后删除或标记删除指定记录。
     */
    /**
     * 删除通知
     */
    public void delete(Long userId, Long notifyId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Notify notify = notifyMapper.selectById(notifyId);
        if (notify == null) {
            throw new BusinessException(ResultCode.NOTIFY_NOT_FOUND);
        }

        if (!notify.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (notifyMapper.deleteByIdAndUserId(notifyId, userId) <= 0) {
            throw new BusinessException(ResultCode.NOTIFY_DELETE_FAILED);
        }

        if (Integer.valueOf(0).equals(notify.getIsRead())) {
            syncUnreadCache(userId);
        }
    }

    /**
     * 同步未读数缓存：用数据库统计值刷新 Redis 缓存。
     */
    private void syncUnreadCache(Long userId) {
        if (stringRedisTemplate == null || userId == null) {
            return;
        }
        Long count = notifyMapper.countUnreadByUserId(userId);
        String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
        stringRedisTemplate.opsForValue().set(
                key,
                String.valueOf(count == null ? 0L : count),
                RedisKeyConstants.NOTIFY_UNREAD_EXPIRE,
                TimeUnit.SECONDS);
        if (multiLevelCacheService != null) {
            multiLevelCacheService.put(key, count == null ? 0L : count);
        }
    }

    /**
     * 从缓存读取未读数：解析失败时返回 0。
     */
    private long getUnreadCountFromCache(String key) {
        if (stringRedisTemplate == null) {
            return 0L;
        }
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null || cached.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(cached));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 解析互动通知类型：根据点赞或收藏动作确定通知类型值。
     */
    private Integer resolveInteractionType(String action) {
        if (MqConstants.ARTICLE_INTERACTION_ACTION_FAVORITE.equals(action)) {
            return NOTIFY_TYPE_FAVORITE;
        }
        return NOTIFY_TYPE_LIKE;
    }

    /**
     * 解析互动动作文案：把动作编码转换成用户能看懂的文字。
     */
    private String resolveInteractionActionText(String action) {
        if (MqConstants.ARTICLE_INTERACTION_ACTION_FAVORITE.equals(action)) {
            return "收藏";
        }
        return "点赞";
    }

    /**
     * 生成默认发送人名称：发送人名称为空时使用兜底文案。
     */
    private String defaultSenderName(String senderName) {
        return senderName == null || senderName.isBlank() ? "有用户" : senderName;
    }

    /**
     * 拼接互动通知内容：生成展示给接收者看的通知正文。
     */
    private String buildInteractionContent(String senderName, String action, String articleTitle, String extraContent) {
        StringBuilder builder = new StringBuilder()
                .append("用户")
                .append(senderName)
                .append("，")
                .append(action)
                .append("了你的文章《")
                .append(articleTitle == null || articleTitle.isBlank() ? "未命名文章" : articleTitle)
                .append("》");
        if (extraContent != null && !extraContent.isBlank()) {
            builder.append("：").append(extraContent);
        }
        return builder.toString();
    }

    /**
     * 转换返回对象：把数据库实体转换成前端需要的 VO。
     */
    private NotifyVO toVO(Notify notify) {
        NotifyVO vo = new NotifyVO();
        vo.setId(notify.getId());
        vo.setType(notify.getType());
        vo.setTitle(notify.getTitle());
        vo.setContent(notify.getContent());
        vo.setArticleId(notify.getArticleId());
        vo.setCommentId(notify.getCommentId());
        vo.setSenderId(notify.getSenderId());
        vo.setIsRead(notify.getIsRead());
        vo.setCreateTime(notify.getCreateTime());
        return vo;
    }
}
