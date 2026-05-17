package com.articleservice.service;

import com.articleservice.entity.Article;
import com.articleservice.entity.ArticleDocument;
import com.articleservice.mapper.ArticleMapper;
import com.articleservice.vo.ArticlePageQueryDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ArticleSearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final ArticleMapper articleMapper;

    /**
     * 构造搜索服务：注入 Elasticsearch 操作对象和文章数据库访问对象。
     */
    public ArticleSearchService(ElasticsearchOperations elasticsearchOperations, ArticleMapper articleMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.articleMapper = articleMapper;
    }

    /**
     * 保存文章到搜索索引：只有正常状态的文章会进入 ES，下架文章会从 ES 删除。
     */
    public void save(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        if (!Integer.valueOf(1).equals(article.getStatus())) {
            delete(article.getId());
            return;
        }
        elasticsearchOperations.save(ArticleDocument.from(article));
    }

    /**
     * 从搜索索引中删除文章：常用于文章删除、下架或状态异常时。
     */
    public void delete(Long articleId) {
        if (articleId != null) {
            elasticsearchOperations.delete(articleId.toString(), ArticleDocument.class);
        }
    }

    /**
     * 按关键词搜索文章：返回命中的文章 id 顺序和总数量，详情再回数据库查询。
     */
    public ArticleSearchResult search(ArticlePageQueryDTO queryDTO) {
        if (queryDTO == null || !StringUtils.hasText(queryDTO.getKeyword())) {
            return new ArticleSearchResult(List.of(), 0L);
        }
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(queryDTO.getKeyword())
                            .fields("title^3", "summary^2", "tags", "content")));
                    b.filter(f -> f.term(t -> t.field("status").value(1)));
                    if (queryDTO.getBoardId() != null) {
                        b.filter(f -> f.term(t -> t.field("boardId").value(queryDTO.getBoardId())));
                    }
                    if (queryDTO.getAuthorId() != null) {
                        b.filter(f -> f.term(t -> t.field("authorId").value(queryDTO.getAuthorId())));
                    }
                    if (Integer.valueOf(1).equals(queryDTO.getOnlyEssence())) {
                        b.filter(f -> f.term(t -> t.field("isEssence").value(1)));
                    }
                    return b;
                }))
                .withPageable(PageRequest.of(queryDTO.getPageNum() - 1, queryDTO.getPageSize()))
                .build();
        SearchHits<ArticleDocument> hits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<Long> ids = hits.stream()
                .map(SearchHit::getContent)
                .map(ArticleDocument::getId)
                .toList();
        return new ArticleSearchResult(ids, hits.getTotalHits());
    }

    /**
     * 重建全部搜索索引：分批读取数据库里的正常文章，然后逐个写入 ES。
     */
    public ReindexResult reindexAll(int pageSize) {
        int safePageSize = pageSize < 1 ? 100 : Math.min(pageSize, 500);
        long indexedCount = 0;
        int page = 0;
        List<Article> batch;
        do {
            int offset = page * safePageSize;
            batch = articleMapper.selectActiveForSearchReindex(offset, safePageSize);
            for (Article article : batch) {
                save(article);
            }
            indexedCount += batch.size();
            page++;
        } while (batch.size() == safePageSize);
        return new ReindexResult(indexedCount, safePageSize);
    }

    /**
     * 搜索结果对象：articleIds 是本页文章 id，total 是符合条件的总数。
     */
    public record ArticleSearchResult(List<Long> articleIds, long total) {
    }

    /**
     * 重建索引结果对象：记录实际写入多少篇文章，以及本次使用的分页大小。
     */
    public record ReindexResult(long indexedCount, int pageSize) {
    }
}
