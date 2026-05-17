package com.notifyservice.controller;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.notifyservice.config.UserContext;
import com.notifyservice.service.MqAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqAdminControllerTest {
    private final MqAdminService mqAdminService = mock(MqAdminService.class);
    private final MqAdminController controller = new MqAdminController(mqAdminService);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getDlqOverviewShouldRejectNonAdmin() {
        UserContext.setRole("USER");

        BusinessException exception = assertThrows(BusinessException.class, controller::getDlqOverview);

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void requeueShouldAllowAdmin() {
        UserContext.setRole("ADMIN");
        when(mqAdminService.requeue("comment", 2))
                .thenReturn(new MqAdminService.RequeueResult("comment", "queue", 2, 1));

        assertEquals(1, controller.requeue("comment", 2).getData().republishedCount());
        verify(mqAdminService).requeue("comment", 2);
    }

    @Test
    void getDlqOverviewShouldAllowAdmin() {
        UserContext.setRole("ADMIN");
        when(mqAdminService.getDlqOverview()).thenReturn(new MqAdminService.DlqOverview(List.of()));

        assertEquals(0, controller.getDlqOverview().getData().queues().size());
        verify(mqAdminService).getDlqOverview();
    }
}
