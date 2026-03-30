package com.articleservice.service;

import com.articleservice.converter.ArticleConverter;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;

import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.PageVO;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
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
    @Autowired(required = false)
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
}