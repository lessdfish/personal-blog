package com.commentservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.logging.DbWriteAuditLogger;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentService {
    @Value("${comment.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    @Value("${comment.rate-limit.window-seconds:60}")
    private long rateLimitWindowSeconds;
    @Value("${comment.rate-limit.threshold:10}")
    private int rateLimitThreshold;

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
     * 创建数据：接收请求参数，校验后保存一条新记录。
     */
    public Long create(Long userId, CommentCreateDTO dto) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        checkRateLimit(userId);

        ArticleSimpleVO article = getArticleOrThrow(dto.getArticleId());
        if (Integer.valueOf(0).equals(article.getAllowComment())) {
            throw new BusinessException(ResultCode.ARTICLE_COMMENT_CLOSED);
        }

        Long notifyUserId;
        Long normalizedParentId = null;
        if (dto.getParentId() == null) {
            notifyUserId = article.getAuthorId();
        } else {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
            }
            if (!Objects.equals(parent.getArticleId(), dto.getArticleId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
            notifyUserId = parent.getUserId();
            normalizedParentId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
        }

        Comment comment = CommentConverter.toEntity(userId, dto);
        comment.setParentId(normalizedParentId);
        comment.setNotifyUserId(notifyUserId);
        if (commentMapper.insert(comment) <= 0) {
            throw new BusinessException(ResultCode.COMMENT_CREATE_FAILED);
        }
        DbWriteAuditLogger.logInsert("tb_comment", comment);
        syncArticleCommentCount(dto.getArticleId(), 1);

        if (!Objects.equals(userId, notifyUserId)) {
            Map<Long, UserSimpleVO> senderMap = getUserMap(List.of(userId));
            UserSimpleVO sender = senderMap.get(userId);
            CommentNotifyMessage message = new CommentNotifyMessage();
            message.setArticleId(dto.getArticleId());
            message.setCommentId(comment.getId());
            message.setSenderId(userId);
            message.setReceiverId(notifyUserId);
            message.setSenderName(sender != null ? sender.getName() : "有用户");
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
     * 分页查询文章评论：按文章 id 返回评论树列表。
     */
    public PageResult<CommentVO> pageByArticle(CommentPageQueryDTO dto) {
        Long articleId = dto.getArticleId();
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long total = commentMapper.countRootCommentsByArticleId(articleId);
        if (total == null || Long.valueOf(0L).equals(total)) {
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

        Set<Long> userIds = new HashSet<>();
        for (Comment root : roots) {
            userIds.add(root.getUserId());
            userIds.add(root.getNotifyUserId());
        }
        for (Comment child : children) {
            userIds.add(child.getUserId());
            userIds.add(child.getNotifyUserId());
        }
        Map<Long, UserSimpleVO> userMap = getUserMap(userIds);
        Map<Long, List<Comment>> childrenMap = children.stream().collect(Collectors.groupingBy(Comment::getParentId));

        List<CommentVO> rootVOs = roots.stream()
                .map(root -> buildRootVO(root, childrenMap, userMap))
                .toList();

        PageResult<CommentVO> result = new PageResult<>();
        result.setTotal(total);
        result.setList(rootVOs);
        return result;
    }

    /**
     * 查询文章评论列表：按文章 id 获取评论及其回复。
     */
    public List<CommentVO> listByArticleId(Long articleId) {
        List<Comment> comments = commentMapper.selectByArticleId(articleId);
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
            userIds.add(comment.getNotifyUserId());
        }
        Map<Long, UserSimpleVO> userMap = getUserMap(userIds);

        return comments.stream().map(comment -> toBaseVO(comment, userMap)).toList();
    }

    /**
     * 删除数据：校验权限后删除或标记删除指定记录。
     */
    public void delete(Long userId, String role, Long id) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        boolean isAdmin = "ADMIN".equals(role);
        if (!isAdmin && !Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        int rows = isAdmin ? commentMapper.deleteById(id) : commentMapper.deleteByIdAndUserId(id, userId);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.COMMENT_DELETE_FAILED);
        }
        syncArticleCommentCount(comment.getArticleId(), -1);
    }

    /**
     * 查询文章并要求存在：文章不存在或不能评论时抛出业务异常。
     */
    private ArticleSimpleVO getArticleOrThrow(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        Result<ArticleSimpleVO> result;
        try {
            result = articleClient.getSimpleById(articleId);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND.getCode(), "无法获取帖子信息: " + e.getMessage());
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND);
        }
        return result.getData();
    }

    /**
     * 同步文章评论数：评论新增或删除后通知文章服务更新计数。
     */
    private void syncArticleCommentCount(Long articleId, Integer delta) {
        try {
            articleClient.updateCommentCount(articleId, delta);
        } catch (Exception e) {
            log.warn("同步文章评论数失败, articleId={}, delta={}", articleId, delta, e);
        }
    }

    /**
     * 批量查询用户信息并整理成 Map：方便给评论补充作者昵称和头像。
     */
    private Map<Long, UserSimpleVO> getUserMap(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<List<UserSimpleVO>> result = userClient.getBatchUserSimple(ids);
        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return Collections.emptyMap();
        }
        return result.getData().stream().collect(Collectors.toMap(UserSimpleVO::getId, user -> user));
    }

    /**
     * 构建一级评论返回对象：把一级评论和它的子回复组装起来。
     */
    private CommentVO buildRootVO(Comment root, Map<Long, List<Comment>> childrenMap, Map<Long, UserSimpleVO> userMap) {
        CommentVO vo = toBaseVO(root, userMap);
        List<Comment> childComments = childrenMap.get(root.getId());
        if (childComments == null || childComments.isEmpty()) {
            vo.setChildren(Collections.emptyList());
            return vo;
        }
        vo.setChildren(childComments.stream().map(child -> {
            CommentVO childVO = toBaseVO(child, userMap);
            childVO.setChildren(Collections.emptyList());
            return childVO;
        }).toList());
        return vo;
    }

    /**
     * 构建评论基础返回对象：把评论实体转换成前端需要的字段。
     */
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

    /**
     * 检查评论频率限制：防止同一用户短时间内频繁发表评论。
     */
    private void checkRateLimit(Long userId) {
        if (!rateLimitEnabled || stringRedisTemplate == null || userId == null) {
            return;
        }
        String key = RedisKeyConstants.LIMIT_COMMENT_KEY + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(count)) {
            stringRedisTemplate.expire(key, rateLimitWindowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > rateLimitThreshold) {
            throw new BusinessException(ResultCode.COMMENT_RATE_LIMIT.getCode(),
                    "评论过于频繁，请" + rateLimitWindowSeconds + "秒后再试");
        }
    }

    /**
     * 查询剩余可评论次数：用于告诉用户当前频率限制还剩多少。
     */
    public int getRemainingComments(Long userId) {
        if (stringRedisTemplate == null || userId == null) {
            return rateLimitThreshold;
        }
        String key = RedisKeyConstants.LIMIT_COMMENT_KEY + userId;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, rateLimitThreshold - count);
    }
}
