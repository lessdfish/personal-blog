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

    public Long create(Long userId, CommentCreateDTO dto) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        checkRateLimit(userId);

        ArticleSimpleVO article = getArticleOrThrow(dto.getArticleId());
        if (article.getAllowComment() != null && article.getAllowComment() == 0) {
            throw new BusinessException(ResultCode.ARTICLE_COMMENT_CLOSED);
        }

        Long notifyUserId;
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
        }

        Comment comment = CommentConverter.toEntity(userId, dto);
        comment.setNotifyUserId(notifyUserId);
        if (commentMapper.insert(comment) <= 0) {
            throw new BusinessException(ResultCode.COMMENT_CREATE_FAILED);
        }
        syncArticleCommentCount(dto.getArticleId(), 1);

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

    public void delete(Long userId, String role, Long id) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        boolean isManager = "ADMIN".equals(role) || "MODERATOR".equals(role);
        int rows = isManager ? commentMapper.deleteById(id) : commentMapper.deleteByIdAndUserId(id, userId);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.COMMENT_DELETE_FAILED);
        }
        syncArticleCommentCount(comment.getArticleId(), -1);
    }

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
        if (result == null || result.getCode() == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND);
        }
        return result.getData();
    }

    private void syncArticleCommentCount(Long articleId, Integer delta) {
        try {
            articleClient.updateCommentCount(articleId, delta);
        } catch (Exception ignored) {
        }
    }

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

    private void checkRateLimit(Long userId) {
        if (!rateLimitEnabled || stringRedisTemplate == null || userId == null) {
            return;
        }
        String key = RedisKeyConstants.LIMIT_COMMENT_KEY + userId;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        if (count >= rateLimitThreshold) {
            throw new BusinessException(ResultCode.COMMENT_RATE_LIMIT.getCode(),
                    "评论过于频繁，请" + rateLimitWindowSeconds + "秒后再试");
        }
        if (count == 0) {
            stringRedisTemplate.opsForValue().set(
                    key, "1", rateLimitWindowSeconds, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

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
