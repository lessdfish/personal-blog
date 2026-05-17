package com.articleservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "blog_article")
public class ArticleDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String summary;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    private Long authorId;
    private Long boardId;
    private Integer status;
    private Integer isEssence;

    /**
     * 把数据库文章对象转换成 Elasticsearch 文档对象，方便后续做关键词搜索。
     */
    public static ArticleDocument from(Article article) {
        ArticleDocument document = new ArticleDocument();
        document.setId(article.getId());
        document.setTitle(article.getTitle());
        document.setSummary(article.getSummary());
        document.setTags(article.getTags());
        document.setContent(article.getContent());
        document.setAuthorId(article.getAuthorId());
        document.setBoardId(article.getBoardId());
        document.setStatus(article.getStatus());
        document.setIsEssence(article.getIsEssence());
        return document;
    }
}
