package com.userservice.vo;

import lombok.Data;

@Data
public class ActiveUserSummaryVO {
    private Long todayActiveUsers;
    private Long weekActiveUsers;
    private Long onlineUsers;
}
