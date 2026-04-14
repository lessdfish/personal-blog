package com.blogcommon.auth;

public record JwtUserInfo(Long userId, String role, String username, String token) {
}
