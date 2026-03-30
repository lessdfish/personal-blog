package com.articleservice.service;

import com.articleservice.converter.ArticleConverter;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;

import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.PageVO;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
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
}