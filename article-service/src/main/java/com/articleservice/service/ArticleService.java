package com.articleservice.service;

import com.articleservice.converter.ArticleConverter;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;

import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.ArticleSimpleVO;
import com.articleservice.vo.PageVO;
import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.util.RedisLockUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleService {
    private static final String ARTICLE_DETAIL_CACHE_KEY = "blog:article:detail:";
    private static final String ARTICLE_HOT_CACHE_KEY = "blog:article:hot:";

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    //publish
    public void publish(Long authorId, ArticlePublishDTO articlePublishDTO) {
        if (authorId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        if (articlePublishDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        if (!StringUtils.hasText(articlePublishDTO.getTitle())) {
            throw new BusinessException(ResultCode.TITLE_NOT_NULL);
        }

        if (!StringUtils.hasText(articlePublishDTO.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_NOT_NULL);
        }

        Article article = new Article();
        article.setTitle(articlePublishDTO.getTitle());
        article.setContent(articlePublishDTO.getContent());
        article.setAuthorId(authorId);
        article.setStatus(1);
        article.setViewCount(0);

        int rows = articleMapper.insert(article);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_PUBLISH_FAILED);
        }
    }

    //page div
    public PageVO<ArticleListVO> pageArticles(ArticlePageQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();

        if (pageNum == null || pageNum < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR1);
        }

        Page<Article> page = PageHelper.startPage(pageNum, pageSize);
        List<Article> articleList = articleMapper.selectPublishedList();

        List<ArticleListVO> voList = articleList.stream()
                .map(ArticleConverter::toArticleListVO)
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(voList);

        return pageVO;
    }

    public ArticleDetailVO getDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        String cacheKey = ARTICLE_DETAIL_CACHE_KEY + id;
        ArticleDetailVO cached = readCache(cacheKey, ArticleDetailVO.class);
        if (cached != null) {
            return cached;
        }

        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }

        articleMapper.incrementViewCount(id);
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        ArticleDetailVO vo = ArticleConverter.toArticleDetailVO(article);
        writeCache(cacheKey, vo, 10, TimeUnit.MINUTES);
        return vo;
    }

    public List<ArticleListVO> listHotArticles(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        String cacheKey = ARTICLE_HOT_CACHE_KEY + safeLimit;
        ArticleListVO[] cached = readCache(cacheKey, ArticleListVO[].class);
        if (cached != null) {
            return List.of(cached);
        }

        List<ArticleListVO> list = articleMapper.selectHotList(safeLimit).stream()
                .map(ArticleConverter::toArticleListVO)
                .toList();
        writeCache(cacheKey, list, 5, TimeUnit.MINUTES);
        return list;
    }

    public ArticleSimpleVO getSimpleById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
        ArticleSimpleVO vo = new ArticleSimpleVO();
        vo.setId(article.getId());
        vo.setAuthorId(article.getAuthorId());
        vo.setTitle(article.getTitle());
        return vo;
    }

    private <T> T readCache(String key, Class<T> targetType) {
        if (stringRedisTemplate == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void writeCache(String key, Object value, long timeout, TimeUnit unit) {
        if (stringRedisTemplate == null || value == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), timeout, unit);
        } catch (JsonProcessingException ignored) {
        }
    }

    // ==================== 点赞功能 ====================

    /**
     * 点赞文章
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 是否点赞成功（true=点赞，false=已点赞过）
     */
    public boolean likeArticle(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        if (stringRedisTemplate == null) {
            throw new BusinessException(ResultCode.REDIS_NOT_RUNNING);
        }

        // 检查文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }

        // 检查是否已点赞
        String likedSetKey = RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(likedSetKey, articleId.toString());
        
        if (Boolean.TRUE.equals(isMember)) {
            // 已点赞，取消点赞
            stringRedisTemplate.opsForSet().remove(likedSetKey, articleId.toString());
            stringRedisTemplate.opsForValue().decrement(RedisKeyConstants.ARTICLE_LIKES_KEY + articleId);
            return false;
        } else {
            // 未点赞，添加点赞
            stringRedisTemplate.opsForSet().add(likedSetKey, articleId.toString());
            stringRedisTemplate.opsForValue().increment(RedisKeyConstants.ARTICLE_LIKES_KEY + articleId);
            return true;
        }
    }

    /**
     * 获取文章点赞数
     */
    public Long getArticleLikes(Long articleId) {
        if (articleId == null) {
            return 0L;
        }
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.ARTICLE_LIKES_KEY + articleId;
            String count = stringRedisTemplate.opsForValue().get(key);
            if (count != null) {
                return Long.parseLong(count);
            }
        }
        return 0L;
    }

    /**
     * 检查用户是否已点赞文章
     */
    public boolean hasLiked(Long userId, Long articleId) {
        if (userId == null || articleId == null || stringRedisTemplate == null) {
            return false;
        }
        String likedSetKey = RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(likedSetKey, articleId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    // ==================== 浏览量功能 ====================

    /**
     * 增加文章浏览量（Redis计数）
     */
    public void incrementViewCount(Long articleId) {
        if (articleId == null) {
            return;
        }
        // 更新数据库
        articleMapper.incrementViewCount(articleId);
        
        // 同时更新Redis缓存
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.ARTICLE_VIEWS_KEY + articleId;
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

    /**
     * 获取文章浏览量（优先从Redis获取）
     */
    public Long getArticleViews(Long articleId) {
        if (articleId == null) {
            return 0L;
        }
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.ARTICLE_VIEWS_KEY + articleId;
            String views = stringRedisTemplate.opsForValue().get(key);
            if (views != null) {
                return Long.parseLong(views);
            }
        }
        // Redis没有，从数据库获取
        Article article = articleMapper.selectById(articleId);
        return article != null && article.getViewCount() != null ? article.getViewCount().longValue() : 0L;
    }

    // ==================== 分布式锁编辑文章 ====================

    /**
     * 编辑文章（带分布式锁）
     */
    public void editArticle(Long userId, Long articleId, ArticlePublishDTO dto) {
        if (userId == null || articleId == null || dto == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }

        // 只有作者才能编辑
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能编辑自己的文章");
        }

        String lockKey = RedisKeyConstants.LOCK_ARTICLE_EDIT_KEY + articleId;
        String lockValue = null;
        
        // 尝试获取分布式锁
        if (stringRedisTemplate != null) {
            lockValue = RedisLockUtil.tryLock(stringRedisTemplate, lockKey, RedisKeyConstants.LOCK_EXPIRE);
            if (lockValue == null) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "文章正在被其他用户编辑，请稍后再试");
            }
        }

        try {
            // 执行编辑操作
            article.setTitle(dto.getTitle());
            article.setContent(dto.getContent());
            // 这里需要添加update方法
            // articleMapper.update(article);
            
            // 清除文章详情缓存
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete(ARTICLE_DETAIL_CACHE_KEY + articleId);
            }
        } finally {
            // 释放锁
            if (lockValue != null && stringRedisTemplate != null) {
                RedisLockUtil.unlock(stringRedisTemplate, lockKey, lockValue);
            }
        }
    }

    /**
     * 删除文章
     */
    public void deleteArticle(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }

        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能删除自己的文章");
        }

        // 软删除
        article.setStatus(0);
        // articleMapper.updateStatus(articleId, 0);
        
        // 清除缓存
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(ARTICLE_DETAIL_CACHE_KEY + articleId);
        }
    }
}