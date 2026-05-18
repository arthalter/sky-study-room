package com.sky.study.controller;

import com.sky.study.exception.BaseException;
import com.sky.study.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public Result<String> test() {
        return Result.success("spring boot ok");
    }
    @GetMapping("/api/test/error")
    public Result<String> testError() {
        throw new BaseException("这是一个自定义异常测试");
    }

    @GetMapping("/api/test/ex")
    public Result<String> testEx() {
        int i = 1 / 0;
        return Result.success("ok");
    }
}