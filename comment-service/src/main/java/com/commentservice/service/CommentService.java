package com.commentservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.message.CommentNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.blogcommon.result.Result;
import com.commentservice.client.ArticleClient;
import com.commentservice.client.UserClient;
import com.commentservice.converter.CommentConverter;
import com.commentservice.dto.CommentCreateDTO;
import com.commentservice.dto.CommentPageQueryDTO;
import com.commentservice.entity.Comment;
import com.commentservice.mapper.CommentMapper;
import com.commentservice.vo.ArticleSimpleVO;
import com.commentservice.vo.CommentVO;
import com.commentservice.vo.PageResult;
import com.commentservice.vo.UserSimpleVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ClassName:CommentService
 * Package:com.commentservice.service
 * Description:评论服务层
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:37
 * @Version: v1.0
 */
@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ArticleClient articleClient;
    @Autowired
    private UserClient userClient;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 创建评论
     */
    public Long create(Long userId, CommentCreateDTO dto) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 评论限流检查
        checkRateLimit(userId);

        ArticleSimpleVO article = getArticleOrThrow(dto.getArticleId());

        Long notifyUserId;
        if (dto.getParentId() == null) {
            // 一级评论，通知文章作者
            notifyUserId = article.getAuthorId();
        } else {
            // 回复评论，通知父评论作者
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
            }
            // 父评论必须属于当前文章
            if (!Objects.equals(parent.getArticleId(), dto.getArticleId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
            notifyUserId = parent.getUserId();
        }
        
        Comment comment = CommentConverter.toEntity(userId, dto);
        comment.setNotifyUserId(notifyUserId);  // 设置被通知用户ID

        if (commentMapper.insert(comment) <= 0) {
            throw new BusinessException(ResultCode.COMMENT_CREATE_FAILED);
        }

        // 发送通知（不通知自己）
        if (!Objects.equals(userId, notifyUserId)) {
            CommentNotifyMessage message = new CommentNotifyMessage();
            message.setArticleId(dto.getArticleId());
            message.setCommentId(comment.getId());
            message.setSenderId(userId);
            message.setReceiverId(notifyUserId);
            message.setArticleTitle(article.getTitle());
            message.setContent(dto.getContent());

            rabbitTemplate.convertAndSend(
                    MqConstants.COMMENT_NOTIFY_EXCHANGE,
                    MqConstants.COMMENT_NOTIFY_ROUTING_KEY,
                    message
            );
        }
        return comment.getId();
    }

    /**
     * 分页查询评论
     */
    public PageResult<CommentVO> pageByArticle(CommentPageQueryDTO dto) {
        Long articleId = dto.getArticleId();
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();

        int offset = (pageNum - 1) * pageSize;

        Long total = commentMapper.countRootCommentsByArticleId(articleId);
        if (total == null || total == 0) {
            PageResult<CommentVO> empty = new PageResult<>();
            empty.setTotal(0L);
            empty.setList(Collections.emptyList());
            return empty;
        }

        List<Comment> roots = commentMapper.selectRootCommentsByArticleId(articleId, offset, pageSize);
        if (roots == null || roots.isEmpty()) {
            PageResult<CommentVO> empty = new PageResult<>();
            empty.setTotal(total);
            empty.setList(Collections.emptyList());
            return empty;
        }

        List<Long> rootIds = roots.stream().map(Comment::getId).toList();

        List<Comment> children = commentMapper.selectChildrenByParentIds(articleId, rootIds);
        if (children == null) {
            children = Collections.emptyList();
        }

        // 收集所有涉及到的用户id
        Set<Long> userIds = new HashSet<>();
        for (Comment root : roots) {
            userIds.add(root.getUserId());
            if (root.getNotifyUserId() != null) {
                userIds.add(root.getNotifyUserId());
            }
        }
        for (Comment child : children) {
            userIds.add(child.getUserId());
            if (child.getNotifyUserId() != null) {
                userIds.add(child.getNotifyUserId());
            }
        }

        Map<Long, UserSimpleVO> userMap = getUserMap(userIds);

        // parentId -> children
        Map<Long, List<Comment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<CommentVO> rootVOs = roots.stream()
                .map(root -> buildRootVO(root, childrenMap, userMap))
                .toList();

        PageResult<CommentVO> result = new PageResult<>();
        result.setTotal(total);
        result.setList(rootVOs);
        return result;
    }

    /**
     * 根据文章ID查询所有评论
     */
    public List<CommentVO> listByArticleId(Long articleId) {
        List<Comment> comments = commentMapper.selectByArticleId(articleId);
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有用户ID
        Set<Long> userIds = new HashSet<>();
        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getNotifyUserId() != null) {
                userIds.add(comment.getNotifyUserId());
            }
        }

        // 批量获取用户信息
        Map<Long, UserSimpleVO> userMap = getUserMap(userIds);

        return comments.stream()
                .map(comment -> {
                    CommentVO vo = CommentConverter.toVO(comment);
                    // 填充用户信息
                    UserSimpleVO user = userMap.get(comment.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getName());
                        vo.setUserAvatar(user.getAvatar());
                    }
                    UserSimpleVO notifyUser = userMap.get(comment.getNotifyUserId());
                    if (notifyUser != null) {
                        vo.setNotifyUserName(notifyUser.getName());
                    }
                    return vo;
                })
                .toList();
    }

    /**
     * 删除评论（只能删除自己的评论）
     */
    public void delete(Long userId, Long id) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (commentMapper.deleteByIdAndUserId(id, userId) <= 0) {
            throw new BusinessException(ResultCode.COMMENT_DELETE_FAILED);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 获取文章信息，如果不存在则抛出异常
     * 同时检查 Result 的 code 是否为成功状态
     */
    private ArticleSimpleVO getArticleOrThrow(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        
        Result<ArticleSimpleVO> result;
        try {
            result = articleClient.getSimpleById(articleId);
        } catch (Exception e) {
            // Feign 调用失败（如 article-service 未启动）
            throw new BusinessException(ResultCode.FAIL.getCode(), 
                    "无法获取文章信息，请确保 article-service 已启动。错误: " + e.getMessage());
        }
        
        if (result == null) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "文章服务返回空响应");
        }
        
        // 检查响应状态码
        if (result.getCode() == null || result.getCode() != 200) {
            String message = result.getMessage() != null ? result.getMessage() : "文章不存在";
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND.getCode(), message);
        }
        
        if (result.getData() == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND);
        }
        
        return result.getData();
    }

    private Map<Long, UserSimpleVO> getUserMap(Collection<Long> userIds) {
        List<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Result<List<UserSimpleVO>> result = userClient.getBatchUserSimple(ids);
        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return Collections.emptyMap();
        }

        return result.getData().stream()
                .collect(Collectors.toMap(UserSimpleVO::getId, user -> user));
    }

    private CommentVO buildRootVO(Comment root,
                                  Map<Long, List<Comment>> childrenMap,
                                  Map<Long, UserSimpleVO> userMap) {
        CommentVO vo = toBaseVO(root, userMap);

        List<Comment> childComments = childrenMap.get(root.getId());
        if (childComments == null || childComments.isEmpty()) {
            vo.setChildren(Collections.emptyList());
            return vo;
        }

        List<CommentVO> childVOs = childComments.stream()
                .map(child -> {
                    CommentVO childVO = toBaseVO(child, userMap);
                    childVO.setChildren(Collections.emptyList());
                    return childVO;
                })
                .toList();

        vo.setChildren(childVOs);
        return vo;
    }

    private CommentVO toBaseVO(Comment comment, Map<Long, UserSimpleVO> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setParentId(comment.getParentId());
        vo.setUserId(comment.getUserId());
        vo.setNotifyUserId(comment.getNotifyUserId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());

        UserSimpleVO user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserAvatar(user.getAvatar());
        }

        UserSimpleVO notifyUser = userMap.get(comment.getNotifyUserId());
        if (notifyUser != null) {
            vo.setNotifyUserName(notifyUser.getName());
        }

        return vo;
    }

    // ==================== 限流功能 ====================

    /**
     * 评论限流检查
     * 每个用户每分钟最多发表 LIMIT_COMMENT_THRESHOLD 条评论
     */
    private void checkRateLimit(Long userId) {
        if (stringRedisTemplate == null || userId == null) {
            return; // Redis未启用，不限流
        }

        String key = RedisKeyConstants.LIMIT_COMMENT_KEY + userId;
        String countStr = stringRedisTemplate.opsForValue().get(key);

        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= RedisKeyConstants.LIMIT_COMMENT_THRESHOLD) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "评论太频繁，请" + RedisKeyConstants.LIMIT_COMMENT_WINDOW + "秒后再试");
        }

        // 增加计数
        if (count == 0) {
            // 第一次，设置key并设置过期时间
            stringRedisTemplate.opsForValue().set(key, "1",
                    RedisKeyConstants.LIMIT_COMMENT_WINDOW, TimeUnit.SECONDS);
        } else {
            // 已有计数，自增
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

    /**
     * 获取用户剩余评论次数
     */
    public int getRemainingComments(Long userId) {
        if (stringRedisTemplate == null || userId == null) {
            return RedisKeyConstants.LIMIT_COMMENT_THRESHOLD;
        }
        String key = RedisKeyConstants.LIMIT_COMMENT_KEY + userId;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, RedisKeyConstants.LIMIT_COMMENT_THRESHOLD - count);
    }
}
