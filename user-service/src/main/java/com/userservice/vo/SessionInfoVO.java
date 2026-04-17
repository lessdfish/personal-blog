package com.userservice.vo;

import lombok.Data;

@Data
public class SessionInfoVO {
    private String loginIp;
    private String location;
    private String device;
    private String browser;
    private String userAgent;
    private String loginTime;
}
