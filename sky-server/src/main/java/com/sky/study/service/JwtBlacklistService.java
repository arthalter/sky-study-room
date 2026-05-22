package com.sky.study.service;

public interface JwtBlacklistService {

    void blacklist(String token);

    boolean isBlacklisted(String token);
}
