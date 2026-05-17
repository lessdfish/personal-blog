package com.blogcommon.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ClassName:JwtUtil
 * Package:com.blogcommon.util
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 21:57
 * @Version: v1.0
 *
 */
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirySeconds;

    public JwtUtil(@Value("${auth.jwt.secret:change-this-jwt-secret-with-at-least-32-random-bytes}") String secret,
                   @Value("${auth.jwt.expiry-seconds:86400}") long expirySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = expirySeconds;
    }

    public String createToken(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        long expire = now + expirySeconds * 1000L;

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(expire))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
