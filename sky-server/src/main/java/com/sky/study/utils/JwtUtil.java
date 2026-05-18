package com.sky.study.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    /**
     * 生成JWT
     * @param secretKey 密钥
     * @param ttl 过期时间（毫秒）
     * @param claims 自定义数据
     * @return token
     */
    public static String createJWT(String secretKey, long ttl, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT
     * @param secretKey 密钥
     * @param token token字符串
     * @return Claims
     */
    public static Claims parseJWT(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
