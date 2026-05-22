package com.sky.study.service.Impl;

import com.sky.study.properties.JwtProperties;
import com.sky.study.service.JwtBlacklistService;
import com.sky.study.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtBlacklistServiceImpl implements JwtBlacklistService {

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public JwtBlacklistServiceImpl(StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void blacklist(String token) {
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
        Date expiration = claims.getExpiration();
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(buildKey(token), "1", Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(token)));
    }

    private String buildKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + sha256(token);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
