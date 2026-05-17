package com.blogcommon.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleEsSyncMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ACTION_UPSERT = "UPSERT";
    public static final String ACTION_DELETE = "DELETE";

    private Long articleId;
    private String action;

    public static ArticleEsSyncMessage upsert(Long articleId) {
        return of(articleId, ACTION_UPSERT);
    }

    public static ArticleEsSyncMessage delete(Long articleId) {
        return of(articleId, ACTION_DELETE);
    }

    private static ArticleEsSyncMessage of(Long articleId, String action) {
        ArticleEsSyncMessage message = new ArticleEsSyncMessage();
        message.setArticleId(articleId);
        message.setAction(action);
        return message;
    }
}
