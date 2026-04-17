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
import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.logging.DbWriteAuditLogger;
import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.blogcommon.result.Result;
import com.blogcommon.util.RedisLockUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleService {
    private static final String ARTICLE_DETAIL_CACHE_KEY = "blog:article:detail:";
    private static final String ARTICLE_HOT_CACHE_KEY = "blog:article:hot:";

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

        if (articleMapper.insert(article) <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_PUBLISH_FAILED);
        }
        DbWriteAuditLogger.logInsert("tb_article", article);
        syncHeat(article.getId());
    }

    public PageVO<ArticleListVO> pageArticles(ArticlePageQueryDTO queryDTO) {
        return pageNormalArticles(queryDTO);
    }

    public PageVO<ArticleListVO> pageNormalArticles(ArticlePageQueryDTO queryDTO) {
        validatePageQuery(queryDTO);
        Page<Article> page = PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<Article> articles = articleMapper.selectPageByCondition(queryDTO);
        List<ArticleListVO> list = articles.stream()
                .map(article -> ArticleConverter.toArticleListVO(article, boardMapper.selectById(article.getBoardId()), getHeat(article.getId())))
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(list);
        return pageVO;
    }

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

    public ArticleDetailVO getDetail(Long id, String viewerKey) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        String cacheKey = ARTICLE_DETAIL_CACHE_KEY + id;
        ArticleDetailVO cached = readCache(cacheKey, ArticleDetailVO.class);
        if (cached != null) {
            recordView(id, viewerKey);
            cached.setViewCount(Math.toIntExact(getArticleViews(id)));
            cached.setHeatScore(getHeat(id));
            cached.setLikeCount(Math.toIntExact(getArticleLikes(id)));
            cached.setFavoriteCount(Math.toIntExact(getArticleFavorites(id)));
            return cached;
        }

        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }

        recordView(id, viewerKey);
        Article refreshed = articleMapper.selectById(id);
        ArticleDetailVO vo = ArticleConverter.toArticleDetailVO(refreshed, boardMapper.selectById(refreshed.getBoardId()), getHeat(id));
        vo.setLikeCount(Math.toIntExact(getArticleLikes(id)));
        vo.setFavoriteCount(Math.toIntExact(getArticleFavorites(id)));
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
        HotArticleSlice slice = getHotArticles(safeLimit);
        List<ArticleListVO> list = buildArticleListVOs(slice.articles(), slice.heatMap());
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
        vo.setAllowComment(article.getAllowComment());
        return vo;
    }

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
                    syncArticleLikeCount(articleId);
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ARTICLE_LIKED_SET_KEY + userId, articleId.toString());
                    }
                    syncHeat(articleId);
                    clearArticleCache(articleId);
                    sendArticleInteractionNotify(userId, articleId, "点赞");
                }
                return true;
            }

            int rows = articleLikeMapper.delete(articleId, userId);
            if (rows > 0) {
                syncArticleLikeCount(articleId);
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
                    syncArticleFavoriteCount(articleId);
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ARTICLE_FAVORITE_SET_KEY + userId, articleId.toString());
                    }
                    syncHeat(articleId);
                    clearArticleCache(articleId);
                    sendArticleInteractionNotify(userId, articleId, "收藏");
                }
                return true;
            }

            int rows = articleFavoriteMapper.delete(articleId, userId);
            if (rows > 0) {
                syncArticleFavoriteCount(articleId);
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

    public PageVO<ArticleListVO> pageMyFavorites(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;

        List<Long> articleIds = articleFavoriteMapper.selectArticleIdsByUser(userId, offset, safePageSize);
        Long total = articleFavoriteMapper.countByUser(userId);
        List<ArticleListVO> list = articleIds == null || articleIds.isEmpty()
                ? List.of()
                : articleMapper.selectByIds(articleIds).stream()
                .map(article -> ArticleConverter.toArticleListVO(article, boardMapper.selectById(article.getBoardId()), getHeat(article.getId())))
                .toList();

        PageVO<ArticleListVO> pageVO = new PageVO<>();
        pageVO.setTotal(total == null ? 0L : total);
        pageVO.setList(list);
        return pageVO;
    }

    public Long getArticleLikes(Long articleId) {
        Long count = articleLikeMapper.countByArticle(articleId);
        return count == null ? 0L : count;
    }

    public Long getArticleFavorites(Long articleId) {
        Long count = articleFavoriteMapper.countByArticle(articleId);
        return count == null ? 0L : count;
    }

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

    public Long getArticleViews(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        return article != null && article.getViewCount() != null ? article.getViewCount().longValue() : 0L;
    }

    public void editArticle(Long userId, String role, Long articleId, ArticlePublishDTO dto) {
        if (userId == null || articleId == null || dto == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        Article article = requireArticle(articleId);
        if (!article.getAuthorId().equals(userId) && !isManager(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        String lockKey = RedisKeyConstants.LOCK_ARTICLE_EDIT_KEY + articleId;
        String lockValue = stringRedisTemplate == null ? "local" : RedisLockUtil.tryLock(
                stringRedisTemplate, lockKey, RedisKeyConstants.LOCK_EXPIRE);
        if (lockValue == null) {
            throw new BusinessException(ResultCode.ARTICLE_EDIT_LOCKED);
        }

        try {
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
        } finally {
            if (stringRedisTemplate != null) {
                RedisLockUtil.unlock(stringRedisTemplate, lockKey, lockValue);
            }
        }
    }

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
    }

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
        articleMapper.updateManageInfo(article);
        syncHeat(articleId);
        clearArticleCache(articleId);
    }

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

    public void updateArticleCommentCount(Long articleId, Integer delta) {
        if (articleId == null || delta == null || delta == 0) {
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

    public Double getHeat(Long articleId) {
        if (stringRedisTemplate == null || articleId == null) {
            return 0D;
        }
        Double heat = stringRedisTemplate.opsForZSet().score(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, articleId.toString());
        if (heat != null) {
            return heat;
        }
        return syncHeat(articleId);
    }

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

    private void recordView(Long articleId, String viewerKey) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
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
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForZSet().add(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY, articleId.toString(), heat);
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.ARTICLE_HEAT_KEY + articleId, String.valueOf(heat));
        }
        return heat;
    }

    private List<ArticleListVO> buildArticleListVOs(List<Article> articles, Map<Long, Double> heatMap) {
        Map<Long, Board> boardCache = new HashMap<>();
        return articles.stream()
                .map(article -> ArticleConverter.toArticleListVO(
                        article,
                        boardCache.computeIfAbsent(article.getBoardId(), boardMapper::selectById),
                        heatMap.getOrDefault(article.getId(), 0D)))
                .toList();
    }

    private HotArticleSlice getHotArticles(int limit) {
        if (stringRedisTemplate == null) {
            List<Article> articles = articleMapper.selectHotList(limit);
            return new HotArticleSlice(articles, buildHeatMapFromArticles(articles));
        }
        return getHotArticlesByPage(0, limit);
    }

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

    private Map<Long, Double> buildHeatMapFromArticles(List<Article> articles) {
        Map<Long, Double> heatMap = new HashMap<>();
        for (Article article : articles) {
            heatMap.put(article.getId(), calculateHeat(article));
        }
        return heatMap;
    }

    private double calculateHeat(Article article) {
        return article.getViewCount() * 1D
                + article.getCommentCount() * 5D
                + article.getLikeCount() * 4D
                + article.getFavoriteCount() * 3D
                + (article.getIsTop() == null ? 0 : article.getIsTop() * 6D)
                + (article.getIsEssence() == null ? 0 : article.getIsEssence() * 10D);
    }

    private record HotArticleSlice(List<Article> articles, Map<Long, Double> heatMap) {
    }

    private Article requireArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_EXIST);
        }
        return article;
    }

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

    private boolean isManager(String role) {
        return "ADMIN".equals(role) || "MODERATOR".equals(role);
    }

    private void clearArticleCache(Long articleId) {
        if (stringRedisTemplate == null) {
            return;
        }
        stringRedisTemplate.delete(ARTICLE_DETAIL_CACHE_KEY + articleId);
        articleAsyncService.evictHotListCaches();
    }

    private void syncArticleLikeCount(Long articleId) {
        Long exactCount = articleLikeMapper.countByArticle(articleId);
        articleMapper.updateLikeCountTo(articleId, exactCount == null ? 0 : exactCount.intValue());
    }

    private void syncArticleFavoriteCount(Long articleId) {
        Long exactCount = articleFavoriteMapper.countByArticle(articleId);
        articleMapper.updateFavoriteCountTo(articleId, exactCount == null ? 0 : exactCount.intValue());
    }

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

    private String getUserName(Long userId) {
        try {
            Result<List<UserSimpleVO>> result = userClient.getBatchUserSimple(List.of(userId));
            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                UserSimpleVO user = result.getData().get(0);
                if (user != null && StringUtils.hasText(user.getName())) {
                    return user.getName();
                }
            }
        } catch (Exception ignored) {
        }
        return "有用户";
    }

    private boolean executeInteractionWithLock(String lockPrefix, Long userId, Long articleId, InteractionAction action) {
        if (stringRedisTemplate == null) {
            return action.execute();
        }
        String lockKey = lockPrefix + userId + ":" + articleId;
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

    private boolean getCurrentState(String lockPrefix, Long userId, Long articleId) {
        if (Objects.equals(lockPrefix, RedisKeyConstants.LOCK_ARTICLE_LIKE_KEY)) {
            return hasLiked(userId, articleId);
        }
        return hasFavorited(userId, articleId);
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

    @FunctionalInterface
    private interface InteractionAction {
        boolean execute();
    }
}
