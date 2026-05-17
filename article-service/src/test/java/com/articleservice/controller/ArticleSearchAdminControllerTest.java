package com.articleservice.controller;

import com.articleservice.config.UserContext;
import com.articleservice.service.ArticleSearchService;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleSearchAdminControllerTest {
    private final ArticleSearchService articleSearchService = mock(ArticleSearchService.class);
    private final ArticleSearchAdminController controller = new ArticleSearchAdminController(articleSearchService);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void reindexShouldRejectNonAdmin() {
        UserContext.setRole("USER");

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.reindex(100));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void reindexShouldAllowAdmin() {
        UserContext.setRole("ADMIN");
        ArticleSearchService.ReindexResult reindexResult = new ArticleSearchService.ReindexResult(3, 100);
        when(articleSearchService.reindexAll(100)).thenReturn(reindexResult);

        Result<ArticleSearchService.ReindexResult> result = controller.reindex(100);

        assertEquals(3L, result.getData().indexedCount());
        verify(articleSearchService).reindexAll(100);
    }
}
