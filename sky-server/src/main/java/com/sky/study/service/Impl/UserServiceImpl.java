package com.sky.study.service.Impl;

import com.sky.study.entity.User;
import com.sky.study.mapper.UserMapper;
import com.sky.study.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper usermapper;

    public User findByUsername(String username) {
        return usermapper.findByUsername(username);
    }
}
