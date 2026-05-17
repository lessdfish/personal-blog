package com.articleservice.repository;

import com.articleservice.entity.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ArticleSearchRepository extends ElasticsearchRepository<ArticleDocument, Long> {
}
