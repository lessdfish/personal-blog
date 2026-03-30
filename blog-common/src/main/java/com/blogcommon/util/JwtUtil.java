package com.blogcommon.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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
public class JwtUtil {
    private static final String SECRET = "blog-cloud-secret-key-blog-cloud-secret-key";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String createToken(Long userId,String username,String role){
        long now = System.currentTimeMillis();
        long expire = now + 1000L * 60 * 60 *24;

        return Jwts.builder()
                .subject(username)
                .claim("userId",userId)
                .claim("role",role)
                .issuedAt(new Date(now))
                .expiration(new Date(expire))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
