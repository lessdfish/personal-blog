package com.notifyservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.message.CommentNotifyMessage;
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

    private static final int NOTIFY_TYPE_COMMENT = 1;

    /**
     * 处理评论通知消息
     */
    public void handleCommentNotify(CommentNotifyMessage message) {
        Notify notify = new Notify();
        notify.setUserId(message.getReceiverId());
        notify.setType(NOTIFY_TYPE_COMMENT);
        notify.setTitle("文章《" + message.getArticleTitle() + "》有新评论");
        notify.setContent(message.getContent());
        notify.setArticleId(message.getArticleId());
        notify.setCommentId(message.getCommentId());
        notify.setSenderId(message.getSenderId());

        notifyMapper.insert(notify);

        // 增加未读计数缓存
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
        }
    }

    /**
     * 减少未读计数缓存
     */
    private void decrementUnreadCache(Long userId, long count) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            stringRedisTemplate.opsForValue().decrement(key, count);
        }
    }

    /**
     * 删除未读计数缓存（标记全部已读时使用）
     */
    private void deleteUnreadCache(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            stringRedisTemplate.delete(key);
        }
    }

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
        if (total == null || total == 0) {
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
     * 获取未读通知数量（带缓存）
     */
    public Long getUnreadCount(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        // 先查Redis缓存
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
        }
        
        // 缓存不存在，查数据库
        Long count = notifyMapper.countUnreadByUserId(userId);
        
        // 写入缓存
        if (stringRedisTemplate != null && count != null) {
            String key = RedisKeyConstants.NOTIFY_UNREAD_KEY + userId;
            stringRedisTemplate.opsForValue().set(key, count.toString(), 
                    RedisKeyConstants.NOTIFY_UNREAD_EXPIRE, TimeUnit.SECONDS);
        }
        
        return count != null ? count : 0L;
    }

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

        if (notifyMapper.markAsRead(notifyId, userId) <= 0) {
            throw new BusinessException(ResultCode.NOTIFY_READ_FAILED);
        }
        
        // 减少未读计数缓存
        decrementUnreadCache(userId, 1);
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        notifyMapper.markAllAsRead(userId);
        
        // 删除未读计数缓存
        deleteUnreadCache(userId);
    }

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
    }

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
