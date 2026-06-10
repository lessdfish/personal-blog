package com.articleservice.service;

import com.articleservice.client.UserClient;
import com.articleservice.converter.ArticleConverter;
import com.articleservice.dto.ArticleManageDTO;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.dto.BoardCreateDTO;
import com.articleservice.entity.Article;
import com.articleservice.entity.ArticleFavorite;
import com.articleservice.entity.ArticleLike;
import com.articleservice.entity.Board;
import com.articleservice.mapper.ArticleFavoriteMapper;
import com.articleservice.mapper.ArticleLikeMapper;
import com.articleservice.mapper.ArticleMapper;
import com.articleservice.mapper.BoardMapper;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.ArticleSimpleVO;
import com.articleservice.vo.BoardVO;
import com.articleservice.vo.PageVO;
import com.articleservice.vo.UserSimpleVO;
import com.blogcommon.auth.RequestUserContext;
import com.blogcommon.cache.MultiLevelCacheService;
import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.lock.DistributedLockService;
import com.blogcommon.logging.DbWriteAuditLogger;
import com.blogcommon.message.ArticleEsSyncMessage;
import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.blogcommon.result.Result;
import com.blogcommon.util.RedisLockUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private BoardMapper boardMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserClient userClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleAsyncService articleAsyncService;
    @Autowired(required = false)
    private ArticleSearchService articleSearchService;
    @Autowired(required = false)
    private MultiLevelCacheService multiLevelCacheService;
    @Autowired(required = false)
    private DistributedLockService distributedLockService;

    /**
     * 发布文章：校验登录和文章内容，保存到数据库，然后刷新热度并发送搜索索引同步消息。
     */
    public void publish(Long authorId, ArticlePublishDTO dto) {
        if (authorId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        validatePublishDTO(dto);
        if (dto.getBoardId() != null && boardMapper.selectById(dto.getBoardId()) == null) {
            throw new BusinessException(ResultCode.BOARD_NOT_EXIST);
        }

        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(buildSummary(dto));
        article.setContent(dto.getContent());
        article.setAuthorId(authorId);
        article.setBoardId(dto.getBoardId());
        article.setTags(dto.getTags());
        article.setStatus(1);
        article.setViewCount(0);
        article.setCommentCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsEssence(0);
        article.setAllowComment(1);
        article.setHotScore(0D);
        article.setHotAdjustScore(0D);
        article.setHotDecayEnabled(1);

        if (articleMapper.insert(article) <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_PUBLISH_FAILED);
        }
        DbWriteAuditLogger.logInsert("tb_article", article);
        syncHeat(article.getId());
        sendArticleEsSync(ArticleEsSyncMessage.upsert(article.getId()));
    }

    /**
     * 分页查询文章：当前只是转发到普通文章分页方法，方便兼容旧调用。
     */
    public PageVO<ArticleListVO> pageArticles(ArticlePageQueryDTO queryDTO) {
        return pageNormalArticles(queryDTO);
    }

    /**
     * 分页查询普通文章：有关键词时优先走 ES 搜索，失败时降级到 MySQL 查询。
     */
    public PageVO<ArticleListVO> pageNormalArticles(ArticlePageQueryDTO queryDTO) {
        validatePageQuery(queryDTO);
        if (StringUtils.hasText(queryDTO.getKeyword()) && articleSearchService != null) {
            try {
                return pageSearchArticles(queryDTO);
            } catch (Exception e) {
                log.warn("ES文章搜索失败，降级到MySQL LIKE查询, keyword={}", queryDTO.getKeyword(), e);
                return pageMysqlArticles(queryDTO, true);
            }
        }
        return pageMysqlArticles(queryDTO, false);
    }

    /**
     * 使用 MySQL 分页查询文章：根据条件查出文章，再补充版块名和热度分数。
     */
    private PageVO<ArticleListVO> pageMysqlArticles(ArticlePageQueryDTO queryDTO, boolean keywordFallback) {
        Page<Article> page = PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<Article> articles = keywordFallback
                ? articleMapper.selectPageByKeywordFallback(queryDTO)
                : articleMapper.selectPageByCondition(queryDTO);
        Map<Long, Board> boardMap = batchLoadBoards(articles);
        List<ArticleListVO> list = articles.stream()
                .map(article -> ArticleConverter.toArticleListVO(article, boardMap.get(article.getBoardId()), getHeat(article.getId())))
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(list);
        return pageVO;
    }

    /**
     * 使用 Elasticsearch 搜索文章：先查出匹配文章 id，再按这个顺序从数据库取完整文章信息。
     */
    private PageVO<ArticleListVO> pageSearchArticles(ArticlePageQueryDTO queryDTO) {
        ArticleSearchService.ArticleSearchResult searchResult = articleSearchService.search(queryDTO);
        List<Long> articleIds = searchResult.articleIds();
        List<Article> articles = articleIds.isEmpty() ? List.of() : articleMapper.selectByIds(articleIds);
        Map<Long, Article> articleMap = articles.stream().collect(Collectors.toMap(Article::getId, article -> article));
        List<Article> sortedArticles = articleIds.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Board> boardMap = batchLoadBoards(sortedArticles);
        List<ArticleListVO> list = sortedArticles.stream()
                .map(article -> ArticleConverter.toArticleListVO(article, boardMap.get(article.getBoardId()), getHeat(article.getId())))
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(searchResult.total());
        pageVO.setList(list);
        return pageVO;
    }

    /**
     * 分页查询热榜文章：按热度排序返回一页文章，并带上总文章数。
     */
    public PageVO<ArticleListVO> pageHotArticles(Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;

        HotArticleSlice slice = getHotArticlesByPage(offset, safePageSize);
        PageVO<ArticleListVO> pageVO = new PageVO<>();
        Long total = articleMapper.countActiveArticles();
        pageVO.setTotal(total == null ? 0L : total);
        pageVO.setList(buildArticleListVOs(slice.articles(), slice.heatMap()));
        return pageVO;
    }

    /**
     * 查询文章详情：优先读缓存，读不到再查数据库，并记录一次有效浏览。
     */
    public ArticleDetailVO getDetail(Long id, String viewerKey) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        String cacheKey = RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + id;
        if (multiLevelCacheService != null) {
            ArticleDetailVO vo = multiLevelCacheService.get(cacheKey, ArticleDetailVO.class,
                    () -> loadArticleDetail(id));
            recordView(id, viewerKey);
            enrichArticleDetailStats(id, vo);
            return vo;
        }
        ArticleDetailVO cached = readCache(cacheKey, ArticleDetailVO.class);
        if (cached != null) {
            recordView(id, viewerKey);
            enrichArticleDetailStats(id, cached);
            return cached;
        }

        ArticleDetailVO vo = loadArticleDetail(id);
        recordView(id, viewerKey);
        writeCache(cacheKey, vo, 10, TimeUnit.MINUTES);
        return vo;
    }

    /**
     * 查询热门文章列表：给首页或侧边栏使用，优先从缓存读取。
     */
    public List<ArticleListVO> listHotArticles(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        String cacheKey = RedisKeyConstants.ARTICLE_HOT_CACHE_KEY + safeLimit;
        if (multiLevelCacheService != null) {
            ArticleListVO[] hotArticles = multiLevelCacheService.get(cacheKey, ArticleListVO[].class,
                    () -> loadHotArticles(safeLimit).toArray(new ArticleListVO[0]));
            return List.of(hotArticles);
        }
        ArticleListVO[] cached = readCache(cacheKey, ArticleListVO[].class);
        if (cached != null) {
            return List.of(cached);
        }
        List<ArticleListVO> list = loadHotArticles(safeLimit);
        writeCache(cacheKey, list, 5, TimeUnit.MINUTES);
        return list;
    }

    /**
     * 查询文章简要信息：给评论服务等其他服务使用，只返回必要字段。
     */
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
        vo.setAllowComment(article.getAllowComment());
        return vo;
    }

    /**
     * 加载文章详情：从数据库读取文章和版块信息，供多级缓存未命中时回源。
     */
    private ArticleDetailVO loadArticleDetail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
        article.setViewCount(article.getViewCount() + 1);
        ArticleDetailVO vo = ArticleConverter.toArticleDetailVO(
                article,
                boardMapper.selectById(article.getBoardId()),
                getHeat(id)
        );
        vo.setLikeCount(Math.toIntExact(getArticleLikes(id)));
        vo.setFavoriteCount(Math.toIntExact(getArticleFavorites(id)));
        return vo;
    }

    /**
     * 补充文章详情动态统计：缓存命中后仍读取浏览、热度、点赞和收藏的最新值。
     */
    private void enrichArticleDetailStats(Long id, ArticleDetailVO vo) {
        if (vo == null) {
            return;
        }
        vo.setViewCount(Math.toIntExact(articleMapper.selectViewCountById(id)));
        vo.setHeatScore(getHeat(id));
        vo.setLikeCount(Math.toIntExact(getArticleLikes(id)));
        vo.setFavoriteCount(Math.toIntExact(getArticleFavorites(id)));
    }

    /**
     * 加载热门文章列表：从热榜或数据库构建前端列表对象。
     */
    private List<ArticleListVO> loadHotArticles(int safeLimit) {
        HotArticleSlice slice = getHotArticles(safeLimit);
        return buildArticleListVOs(slice.articles(), slice.heatMap());
    }

    /**
     * 设置点赞状态：targetLiked 为 true 表示点赞，为 false 表示取消点赞。
     */
    public boolean setArticleLikeStatus(Long userId, Long articleId, boolean targetLiked) {
        if (userId == null || articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        requireArticle(articleId);
        return executeInteractionWithLock(RedisKeyConstants.LOCK_ARTICLE_LIKE_KEY, userId, articleId, () -> {
            if (targetLiked) {
                ArticleLike articleLike = new ArticleLike();
                articleLike.setArticleId(articleId);
                articleLike.setUserId(userId);
                int rows = articleLikeMapper.insertIgnore(articleLike);
                if (rows > 0) {
                    DbWriteAuditLogger.logInsert("tb_article_like", articleLike);
                    syncArticleLikeCount(articleId, true);
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId, articleId.toString());
                    }
                    syncHeat(articleId);
                    clearArticleCache(articleId);
                    sendArticleInteractionNotify(
                            userId,
                            articleId,
                            MqConstants.ARTICLE_INTERACTION_ACTION_LIKE
                    );
                }
                return true;
            }

            int rows = articleLikeMapper.delete(articleId, userId);
            if (rows > 0) {
                syncArticleLikeCount(articleId, false);
                if (stringRedisTemplate != null) {
                    stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId, articleId.toString());
                }
                syncHeat(articleId);
                clearArticleCache(articleId);
            } else if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId, articleId.toString());
            }
            return false;
        });
    }

    /**
     * 设置收藏状态：targetFavorited 为 true 表示收藏，为 false 表示取消收藏。
     */
    public boolean setArticleFavoriteStatus(Long userId, Long articleId, boolean targetFavorited) {
        if (userId == null || articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        requireArticle(articleId);
        return executeInteractionWithLock(RedisKeyConstants.LOCK_ARTICLE_FAVORITE_KEY, userId, articleId, () -> {
            if (targetFavorited) {
                ArticleFavorite favorite = new ArticleFavorite();
                favorite.setArticleId(articleId);
                favorite.setUserId(userId);
                int rows = articleFavoriteMapper.insertIgnore(favorite);
                if (rows > 0) {
                    DbWriteAuditLogger.logInsert("tb_article_favorite", favorite);
                    syncArticleFavoriteCount(articleId, true);
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ARTICLE_FAVORITE_SET_KEY + userId, articleId.toString());
                    }
                    syncHeat(articleId);
                    clearArticleCache(articleId);
                    sendArticleInteractionNotify(
                            userId,
                            articleId,
                            MqConstants.ARTICLE_INTERACTION_ACTION_FAVORITE
                    );
                }
                return true;
            }

            int rows = articleFavoriteMapper.delete(articleId, userId);
            if (rows > 0) {
                syncArticleFavoriteCount(articleId, false);
                if (stringRedisTemplate != null) {
                    stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ARTICLE_FAVORITE_SET_KEY + userId, articleId.toString());
                }
                syncHeat(articleId);
                clearArticleCache(articleId);
            } else if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ARTICLE_FAVORITE_SET_KEY + userId, articleId.toString());
            }
            return false;
        });
    }

    /**
     * 判断用户是否收藏文章：优先查 Redis 缓存，缓存没有再查数据库。
     */
    public boolean hasFavorited(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            return false;
        }
        if (stringRedisTemplate != null) {
            Boolean member = stringRedisTemplate.opsForSet()
                    .isMember(RedisKeyConstants.ARTICLE_FAVORITE_SET_KEY + userId, articleId.toString());
            if (Boolean.TRUE.equals(member)) {
                return true;
            }
        }
        Long count = articleFavoriteMapper.countByArticleAndUser(articleId, userId);
        return count != null && count > 0;
    }

    /**
     * 分页查询我的收藏：根据用户 id 查出收藏过的文章列表。
     */
    public PageVO<ArticleListVO> pageMyFavorites(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;

        List<Long> articleIds = articleFavoriteMapper.selectArticleIdsByUser(userId, offset, safePageSize);
        Long total = articleFavoriteMapper.countByUser(userId);
        List<Article> articles = articleIds == null || articleIds.isEmpty()
                ? List.of()
                : articleMapper.selectByIds(articleIds);
        Map<Long, Board> boardMap = batchLoadBoards(articles);
        List<ArticleListVO> list = articles.stream()
                .map(article -> ArticleConverter.toArticleListVO(article, boardMap.get(article.getBoardId()), getHeat(article.getId())))
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(total == null ? 0L : total);
        pageVO.setList(list);
        return pageVO;
    }

    /**
     * 查询文章点赞数：从点赞表统计，查不到时返回 0。
     */
    public Long getArticleLikes(Long articleId) {
        Long count = articleLikeMapper.countByArticle(articleId);
        return count == null ? 0L : count;
    }

    /**
     * 查询文章收藏数：从收藏表统计，查不到时返回 0。
     */
    public Long getArticleFavorites(Long articleId) {
        Long count = articleFavoriteMapper.countByArticle(articleId);
        return count == null ? 0L : count;
    }

    /**
     * 判断用户是否点赞文章：优先查 Redis 缓存，缓存没有再查数据库。
     */
    public boolean hasLiked(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            return false;
        }
        if (stringRedisTemplate != null) {
            Boolean isMember = stringRedisTemplate.opsForSet().isMember(
                    RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId, articleId.toString());
            if (Boolean.TRUE.equals(isMember)) {
                return true;
            }
        }
        Long count = articleLikeMapper.countByArticleAndUser(articleId, userId);
        return count != null && count > 0;
    }

    /**
     * 查询文章浏览量：直接读取文章表里的 view_count。
     */
    public Long getArticleViews(Long articleId) {
        Integer viewCount = articleMapper.selectViewCountById(articleId);
        return viewCount == null ? 0L : viewCount.longValue();
    }

    /**
     * 编辑文章：作者本人或管理员可以修改文章内容，并同步清理缓存和搜索索引。
     */
    public void editArticle(Long userId, String role, Long articleId, ArticlePublishDTO dto) {
        if (userId == null || articleId == null || dto == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        Article article = requireArticle(articleId);
        if (!article.getAuthorId().equals(userId) && !isManager(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        String lockKey = RedisKeyConstants.LOCK_ARTICLE_EDIT_KEY + articleId;
        if (distributedLockService != null) {
            String lockValue = distributedLockService.tryLock(lockKey, Duration.ofSeconds(RedisKeyConstants.LOCK_EXPIRE));
            if (lockValue == null) {
                throw new BusinessException(ResultCode.ARTICLE_EDIT_LOCKED);
            }
            try {
                updateArticleContent(article, dto, articleId);
                return;
            } finally {
                distributedLockService.unlock(lockKey, lockValue);
            }
        }
        String lockValue = stringRedisTemplate == null ? "local" : RedisLockUtil.tryLock(
                stringRedisTemplate, lockKey, RedisKeyConstants.LOCK_EXPIRE);
        if (lockValue == null) {
            throw new BusinessException(ResultCode.ARTICLE_EDIT_LOCKED);
        }

        try {
            updateArticleContent(article, dto, articleId);
        } finally {
            if (stringRedisTemplate != null) {
                RedisLockUtil.unlock(stringRedisTemplate, lockKey, lockValue);
            }
        }
    }

    /**
     * 删除文章：作者本人或管理员可以把文章状态改为删除，并同步热榜、缓存和搜索索引。
     */
    public void deleteArticle(Long userId, String role, Long articleId) {
        if (userId == null || articleId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        Article article = requireArticle(articleId);
        if (!article.getAuthorId().equals(userId) && !isManager(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (articleMapper.updateStatus(articleId, 0) <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_DELETE_FAILED);
        }
        syncHeat(articleId);
        clearArticleCache(articleId);
        sendArticleEsSync(ArticleEsSyncMessage.delete(articleId));
    }

    /**
     * 管理文章：管理员调整置顶、精华、评论开关或状态。
     */
    public void manageArticle(String role, Long articleId, ArticleManageDTO dto) {
        if (!isManager(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Article article = requireArticle(articleId);
        if (dto.getIsTop() != null) {
            article.setIsTop(dto.getIsTop());
        }
        if (dto.getIsEssence() != null) {
            article.setIsEssence(dto.getIsEssence());
        }
        if (dto.getAllowComment() != null) {
            article.setAllowComment(dto.getAllowComment());
        }
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
        }
        if (dto.getHotAdjustScore() != null) {
            article.setHotAdjustScore(dto.getHotAdjustScore());
        }
        if (dto.getHotDecayEnabled() != null) {
            article.setHotDecayEnabled(dto.getHotDecayEnabled());
        }
        articleMapper.updateManageInfo(article);
        syncHeat(articleId);
        clearArticleCache(articleId);
        sendArticleEsSync(ArticleEsSyncMessage.upsert(articleId));
    }

    /**
     * 创建版块：管理员新增一个可发帖的版块，并检查版块编码不能重复。
     */
    public void createBoard(String role, BoardCreateDTO dto) {
        if (!isManager(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (boardMapper.selectByCode(dto.getBoardCode()) != null) {
            throw new BusinessException(ResultCode.BOARD_CODE_EXIST);
        }
        Board board = new Board();
        board.setBoardName(dto.getBoardName());
        board.setBoardCode(dto.getBoardCode());
        board.setDescription(dto.getDescription());
        board.setSortOrder(dto.getSortOrder() == null ? 99 : dto.getSortOrder());
        board.setStatus(1);
        boardMapper.insert(board);
        DbWriteAuditLogger.logInsert("tb_board", board);
    }

    /**
     * 查询版块列表：把数据库里的启用版块转换成前端需要的 BoardVO。
     */
    public List<BoardVO> listBoards() {
        return boardMapper.selectEnabledList().stream().map(board -> {
            BoardVO vo = new BoardVO();
            vo.setId(board.getId());
            vo.setBoardName(board.getBoardName());
            vo.setBoardCode(board.getBoardCode());
            vo.setDescription(board.getDescription());
            vo.setSortOrder(board.getSortOrder());
            return vo;
        }).toList();
    }

    /**
     * 更新文章评论数：评论服务告诉文章服务评论增加或减少后，重新计算评论数量。
     */
    public void updateArticleCommentCount(Long articleId, Integer delta) {
        if (articleId == null || delta == null || Integer.valueOf(0).equals(delta)) {
            return;
        }
        Article article = articleMapper.selectAnyById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
        int current = article.getCommentCount() == null ? 0 : article.getCommentCount();
        int target = Math.max(0, current + delta);
        int rows = articleMapper.updateCommentCountTo(articleId, target);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_UPDATE_FAILED);
        }
        syncHeat(articleId);
        clearArticleCache(articleId);
    }

    /**
     * 查询文章热度：优先从 Redis 热度榜读取，读不到就重新计算。
     */
    public Double getHeat(Long articleId) {
        if (articleId == null) {
            return 0D;
        }
        if (stringRedisTemplate == null) {
            Article article = articleMapper.selectAnyById(articleId);
            return article == null ? 0D : calculateHeat(article);
        }
        Double heat = stringRedisTemplate.opsForZSet().score(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, articleId.toString());
        if (heat != null) {
            return heat;
        }
        return syncHeat(articleId);
    }

    /**
     * 校验发布或编辑文章的内容：标题和正文不能为空。
     */
    private void validatePublishDTO(ArticlePublishDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException(ResultCode.TITLE_NOT_NULL);
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_NOT_NULL);
        }
    }

    /**
     * 更新文章正文信息：保存编辑结果，并同步清理缓存和搜索索引。
     */
    private void updateArticleContent(Article article, ArticlePublishDTO dto, Long articleId) {
        validatePublishDTO(dto);
        article.setTitle(dto.getTitle());
        article.setSummary(buildSummary(dto));
        article.setContent(dto.getContent());
        article.setBoardId(dto.getBoardId());
        article.setTags(dto.getTags());
        if (articleMapper.updateArticle(article) <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_UPDATE_FAILED);
        }
        clearArticleCache(articleId);
        sendArticleEsSync(ArticleEsSyncMessage.upsert(articleId));
    }

    /**
     * 校验分页参数：页码和每页条数都必须是正数。
     */
    private void validatePageQuery(ArticlePageQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR1);
        }
    }

    /**
     * 记录浏览量：同一个用户或 IP 在 10 分钟内只算一次有效浏览。
     */
    private void recordView(Long articleId, String viewerKey) {
        if (stringRedisTemplate == null || !StringUtils.hasText(viewerKey)) {
            articleMapper.incrementViewCount(articleId);
            syncHeat(articleId);
            clearArticleCache(articleId);
            return;
        }
        String key = RedisKeyConstants.ARTICLE_VIEWED_KEY + articleId + ":" + viewerKey;
        Boolean firstView = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        if (Boolean.TRUE.equals(firstView)) {
            articleMapper.incrementViewCount(articleId);
            syncHeat(articleId);
            clearArticleCache(articleId);
        }
    }

    /**
     * 同步文章热度：重新计算热度并写入 Redis 热度榜；文章无效时从榜单移除。
     */
    private double syncHeat(Long articleId) {
        if (articleId == null) {
            return 0D;
        }
        Article article = articleMapper.selectAnyById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, articleId.toString());
                stringRedisTemplate.delete(RedisKeyConstants.ARTICLE_HEAT_KEY + articleId);
            }
            return 0D;
        }
        double heat = calculateHeat(article);
        articleMapper.updateHeatSnapshot(articleId, heat, LocalDateTime.now());
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForZSet().add(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, articleId.toString(), heat);
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.ARTICLE_HEAT_KEY + articleId, String.valueOf(heat));
        }
        return heat;
    }

    /**
     * 批量加载版块信息：文章列表里有多个版块时，一次性查出版块，减少数据库查询次数。
     */
    private Map<Long, Board> batchLoadBoards(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> boardIds = articles.stream()
                .map(Article::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (boardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Board> boards = boardMapper.selectByIds(boardIds);
        return boards.stream().collect(Collectors.toMap(Board::getId, b -> b));
    }

    /**
     * 构建文章列表返回对象：把 Article 加上版块名和热度，转换成前端列表需要的 VO。
     */
    private List<ArticleListVO> buildArticleListVOs(List<Article> articles, Map<Long, Double> heatMap) {
        Map<Long, Board> boardCache = new HashMap<>();
        return articles.stream()
                .map(article -> ArticleConverter.toArticleListVO(
                        article,
                        boardCache.computeIfAbsent(article.getBoardId(), boardMapper::selectById),
                        heatMap.getOrDefault(article.getId(), 0D)))
                .toList();
    }

    /**
     * 查询热榜前 N 篇文章：有 Redis 时走热度榜，没有 Redis 时直接查数据库。
     */
    private HotArticleSlice getHotArticles(int limit) {
        if (stringRedisTemplate == null) {
            List<Article> articles = articleMapper.selectHotList(limit);
            return new HotArticleSlice(articles, buildHeatMapFromArticles(articles));
        }
        return getHotArticlesByPage(0, limit);
    }

    /**
     * 分页查询热榜文章：从 Redis 热度有序集合里取文章 id，再回数据库补全文章信息。
     */
    private HotArticleSlice getHotArticlesByPage(int offset, int pageSize) {
        if (stringRedisTemplate == null) {
            List<Article> articles = articleMapper.selectHotPage(offset, pageSize);
            return new HotArticleSlice(articles, buildHeatMapFromArticles(articles));
        }
        ensureHotRankCache(Math.max(2000, offset + pageSize));
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> hotTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, offset, offset + pageSize - 1L);
        if (hotTuples == null || hotTuples.isEmpty()) {
            List<Article> articles = articleMapper.selectHotPage(offset, pageSize);
            return new HotArticleSlice(articles, buildHeatMapFromArticles(articles));
        }
        Map<Long, Double> heatMap = new HashMap<>();
        List<Long> ids = hotTuples.stream()
                .map(tuple -> {
                    Long articleId = Long.valueOf(tuple.getValue());
                    heatMap.put(articleId, tuple.getScore() == null ? 0D : tuple.getScore());
                    return articleId;
                })
                .toList();
        List<Article> articles = articleMapper.selectByIds(ids);
        articles.sort((a, b) -> Double.compare(
                heatMap.getOrDefault(b.getId(), 0D),
                heatMap.getOrDefault(a.getId(), 0D)));
        return new HotArticleSlice(articles, heatMap);
    }

    /**
     * 确保热榜缓存足够多：缓存不足时异步预热，避免热榜页拿不到数据。
     */
    private void ensureHotRankCache(int warmLimit) {
        if (stringRedisTemplate == null) {
            return;
        }
        Long size = stringRedisTemplate.opsForZSet().zCard(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY);
        if (size != null && size >= warmLimit) {
            return;
        }
        articleAsyncService.warmHotRankCache(warmLimit);
    }

    /**
     * 从文章列表计算热度映射：把每篇文章 id 对应到它的热度分数。
     */
    private Map<Long, Double> buildHeatMapFromArticles(List<Article> articles) {
        Map<Long, Double> heatMap = new HashMap<>();
        for (Article article : articles) {
            heatMap.put(article.getId(), calculateHeat(article));
        }
        return heatMap;
    }

    /**
     * 计算文章热度分数：浏览、评论、点赞、收藏、置顶和精华都会影响排序。
     */
    private double calculateHeat(Article article) {
        return ArticleHeatCalculator.calculate(article);
    }

    /**
     * 热榜查询结果：articles 是文章列表，heatMap 保存每篇文章对应的热度分数。
     */
    private record HotArticleSlice(List<Article> articles, Map<Long, Double> heatMap) {
    }

    /**
     * 查询并要求文章存在：不存在就抛出“文章不存在”的业务异常。
     */
    private Article requireArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
        return article;
    }

    /**
     * 生成文章摘要：优先使用用户填写的摘要，没有摘要时截取正文前 120 个字符。
     */
    private String buildSummary(ArticlePublishDTO dto) {
        if (StringUtils.hasText(dto.getSummary())) {
            return dto.getSummary();
        }
        String content = dto.getContent();
        if (!StringUtils.hasText(content)) {
            return null;
        }
        return content.length() <= 120 ? content : content.substring(0, 120);
    }

    /**
     * 判断是否管理员：当前只有 ADMIN 角色算管理员。
     */
    private boolean isManager(String role) {
        return "ADMIN".equals(role);
    }

    /**
     * 清理文章相关缓存：删除详情缓存，并异步删除热榜列表缓存。
     */
    private void clearArticleCache(Long articleId) {
        String detailKey = RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + articleId;
        if (multiLevelCacheService != null) {
            multiLevelCacheService.evict(detailKey);
            multiLevelCacheService.evictByPrefix(RedisKeyConstants.ARTICLE_HOT_CACHE_KEY);
        }
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(detailKey);
            articleAsyncService.evictHotListCaches();
        }
    }

    /**
     * 同步文章点赞数量：点赞加 1，取消点赞减 1。
     */
    private void syncArticleLikeCount(Long articleId, boolean liked) {
        articleMapper.incrementLikeCount(articleId, liked ? 1 : -1);
    }

    /**
     * 同步文章收藏数量：收藏加 1，取消收藏减 1。
     */
    private void syncArticleFavoriteCount(Long articleId, boolean favorited) {
        articleMapper.incrementFavoriteCount(articleId, favorited ? 1 : -1);
    }

    /**
     * 发送文章互动通知：别人点赞或收藏文章时，通知文章作者。
     */
    private void sendArticleInteractionNotify(Long senderId, Long articleId, String action) {
        if (rabbitTemplate == null) {
            return;
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getAuthorId() == null || Objects.equals(article.getAuthorId(), senderId)) {
            return;
        }

        ArticleInteractionNotifyMessage message = new ArticleInteractionNotifyMessage();
        message.setArticleId(articleId);
        message.setSenderId(senderId);
        message.setReceiverId(article.getAuthorId());
        message.setSenderName(getUserName(senderId));
        message.setArticleTitle(article.getTitle());
        message.setAction(action);
        rabbitTemplate.convertAndSend(
                MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE,
                MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY,
                message
        );
    }

    /**
     * 发送文章搜索同步消息：告诉消费者某篇文章需要新增、更新或删除 ES 索引。
     */
    private void sendArticleEsSync(ArticleEsSyncMessage message) {
        if (rabbitTemplate == null || message == null || message.getArticleId() == null) {
            return;
        }
        rabbitTemplate.convertAndSend(
                MqConstants.ARTICLE_ES_SYNC_EXCHANGE,
                MqConstants.ARTICLE_ES_SYNC_ROUTING_KEY,
                message
        );
    }

    /**
     * 查询用户名：调用用户服务获取昵称，失败时返回一个默认文案。
     */
    private String getUserName(Long userId) {
        String username = RequestUserContext.getUsername();
        if (StringUtils.hasText(username)) {
            return username;
        }
        if (userClient == null) {
            return "有用户";
        }
        try {
            Result<List<UserSimpleVO>> result = userClient.getBatchUserSimple(List.of(userId));
            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                UserSimpleVO user = result.getData().get(0);
                if (user != null && StringUtils.hasText(user.getName())) {
                    return user.getName();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户名失败, userId={}", userId, e);
        }
        return "有用户";
    }

    /**
     * 带分布式锁执行点赞或收藏：避免用户连续点击导致数据重复或数量不准。
     */
    private boolean executeInteractionWithLock(String lockPrefix, Long userId, Long articleId, InteractionAction action) {
        String lockKey = lockPrefix + userId + ":" + articleId;
        if (distributedLockService != null) {
            return distributedLockService.executeWithLock(
                    lockKey,
                    Duration.ofSeconds(5),
                    action::execute,
                    () -> getCurrentState(lockPrefix, userId, articleId)
            );
        }
        if (stringRedisTemplate == null) {
            return action.execute();
        }
        String lockValue = RedisLockUtil.tryLockWithRetry(stringRedisTemplate, lockKey, 5, 3, 50);
        if (!StringUtils.hasText(lockValue)) {
            return getCurrentState(lockPrefix, userId, articleId);
        }
        try {
            return action.execute();
        } finally {
            RedisLockUtil.unlock(stringRedisTemplate, lockKey, lockValue);
        }
    }

    /**
     * 获取当前互动状态：拿不到锁时，用已有数据判断当前是点赞还是收藏状态。
     */
    private boolean getCurrentState(String lockPrefix, Long userId, Long articleId) {
        if (Objects.equals(lockPrefix, RedisKeyConstants.LOCK_ARTICLE_LIKE_KEY)) {
            return hasLiked(userId, articleId);
        }
        return hasFavorited(userId, articleId);
    }

    /**
     * 读取 Redis 缓存：把缓存里的 JSON 字符串转换成指定类型的 Java 对象。
     */
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

    /**
     * 写入 Redis 缓存：把 Java 对象转换成 JSON 后保存，并设置过期时间。
     */
    private void writeCache(String key, Object value, long timeout, TimeUnit unit) {
        if (stringRedisTemplate == null || value == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), timeout, unit);
        } catch (JsonProcessingException ignored) {
        }
    }

    @FunctionalInterface
    private interface InteractionAction {
        /**
         * 执行一次具体互动操作：可能是点赞、取消点赞、收藏或取消收藏。
         */
        boolean execute();
    }
}
