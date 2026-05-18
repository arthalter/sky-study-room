package com.sky.study.service;

import com.sky.study.entity.User;

public interface UserService {
    User findByUsername(String username);
}
