package com.sky.study.vo;

import lombok.Data;

@Data
public class UserLoginVO {

    private String Token;
    private String username;
    private String role;
    private Long id;

}
