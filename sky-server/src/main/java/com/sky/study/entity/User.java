package com.sky.study.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：user
 */
@Data
public class User {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码（加密存储）
     */
    private String password;

    /**
     * 用户角色
     * USER / ADMIN
     */
    private String role;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
